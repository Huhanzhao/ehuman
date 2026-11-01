package com.ehuman.plugin;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 插件管理器 - 负责加载、卸载、调用插件
 */
@Component
@Slf4j
public class VoicePluginManager {

    private static final String BOOT_LIB_DIR = "BOOT-INF/lib/";
    private static final String BOOT_CLASSES_DIR = "BOOT-INF/classes/";

    @Autowired
    private SystemPath systemPath;

    // 存储所有已加载的插件
    private final Map<String, TtsPlugin> plugins = new ConcurrentHashMap<>();
    
    // 存储插件源码文件路径
    private final Map<String, Path> pluginSourcePaths = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() throws Exception {
        log.info("========== 插件管理器初始化 ==========");
        log.info("插件源码目录: {}", Paths.get(systemPath.getVoicePluginSourcePath()).toAbsolutePath());
        log.info("插件编译目录: {}", Paths.get(systemPath.getVoicePluginClassesPath()).toAbsolutePath());

        // 先清理上次插件编译文件
        cleanClassDirectory();

        // 加载所有插件
        loadAllPlugins();

        log.info("====================================");
    }
    
    /**
     * 加载所有插件
     */
    public void loadAllPlugins() throws Exception {
        log.info("开始加载所有插件...");
        
        // 1. 清空现有插件
        plugins.clear();
        pluginSourcePaths.clear();
        
        // 2. 查找所有Java文件
        List<Path> javaFiles = findJavaFiles(Paths.get(systemPath.getVoicePluginSourcePath()));
        if (javaFiles.isEmpty()) {
            log.warn("未找到任何插件文件");
            return;
        }
        
        log.info("发现 {} 个插件文件", javaFiles.size());
        
        // 3. 编译所有Java文件
        boolean compileSuccess = compileJavaFiles(javaFiles);
        if (!compileSuccess) {
            log.error("编译失败，无法加载插件");
            return;
        }
        
        // 4. 加载编译后的类
        loadCompiledClasses();
        
        log.info("插件加载完成，共加载 {} 个插件: {}", plugins.size(), plugins.keySet());
    }
    
    /**
     * 编译Java文件
     */
    private boolean compileJavaFiles(List<Path> javaFiles) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            log.error("未找到Java编译器，请使用JDK而不是JRE运行");
            return false;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(javaFiles.stream()
                        .map(Path::toFile)
                        .collect(Collectors.toList()));

        // 获取动态编译可用的 classpath（兼容 Spring Boot Fat Jar）
        CompileClasspath compileClasspath = buildCompileClasspath();

        List<String> options = new ArrayList<>(List.of(
                "-d", systemPath.getVoicePluginClassesPath(),
                "-cp", compileClasspath.classpath(),
                "-encoding", "UTF-8"
        ));
        if (compileClasspath.processorPath() != null) {
            // 显式指定 lombok，避免 JDK 21+ 在 classpath 上隐式启用注解处理器的告警
            options.add("-proc:full");
            options.add("-processorpath");
            options.add(compileClasspath.processorPath());
        }

        log.debug("编译参数: {}", options);

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits
        );

        boolean success = task.call();
        
        // 打印编译错误
        if (!success) {
            log.error("编译失败，错误信息:");
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                log.error("  {}", diagnostic.toString());
            }
        } else {
            log.info("编译成功，共编译 {} 个文件", javaFiles.size());
        }
        
        try {
            fileManager.close();
        } catch (IOException e) {
            log.error("关闭FileManager失败", e);
        }
        
        return success;
    }


    /**
     * 动态编译使用的 classpath 与注解处理器路径
     */
    private record CompileClasspath(String classpath, String processorPath) {
    }

    /**
     * 构建动态编译使用的 classpath。
     * <p>
     * Spring Boot Fat Jar 里的依赖全部在 BOOT-INF/lib 下（jar 里嵌套 jar），
     * 它们不是文件系统上的可访问路径，javac 无法直接读取，所以需要先解压到临时目录。
     * 解压结果按 Fat Jar 的路径/大小/修改时间缓存，只有 jar 变化时才会重新解压。
     * 在 IDE 或普通 jar（依赖都在外部目录）中运行时，直接使用 java.class.path。
     */
    private CompileClasspath buildCompileClasspath() {
        Path fatJar = findFatJar();
        if (fatJar != null) {
            try {
                Path extractedDir = extractFatJarClasspath(fatJar);
                log.info("检测到 Fat Jar，动态编译使用解压后的依赖: {}", extractedDir);
                return new CompileClasspath(
                        buildExtractedClasspath(extractedDir),
                        findLombokJar(extractedDir)
                );
            } catch (Exception e) {
                log.warn("解压 Fat Jar 依赖失败，回退到 java.class.path", e);
            }
        }
        return new CompileClasspath(System.getProperty("java.class.path"), null);
    }

    /**
     * 从启动 classpath 中找到 Spring Boot Fat Jar
     */
    private Path findFatJar() {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.isBlank()) {
            return null;
        }
        for (String entry : classpath.split(File.pathSeparator)) {
            if (!entry.endsWith(".jar")) {
                continue;
            }
            Path jar = Paths.get(entry);
            if (Files.isRegularFile(jar) && isFatJar(jar)) {
                return jar.toAbsolutePath();
            }
        }
        return null;
    }

    /**
     * 判断一个 jar 是否是 Spring Boot Fat Jar（包含 BOOT-INF/lib 下的依赖）
     */
    private boolean isFatJar(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(BOOT_LIB_DIR) && name.endsWith(".jar")) {
                    return true;
                }
            }
        } catch (IOException e) {
            log.debug("读取 jar 失败，忽略: {}", jar, e);
        }
        return false;
    }

    /**
     * 把 Fat Jar 中的 BOOT-INF/classes 和 BOOT-INF/lib 解压到临时目录
     */
    private Path extractFatJarClasspath(Path fatJar) throws IOException {
        String key = fatJar.getFileName() + "-" + Files.size(fatJar) + "-"
                + Files.getLastModifiedTime(fatJar).toMillis();
        key = key.replaceAll("[^A-Za-z0-9._-]", "_");

        Path cacheRoot = Paths.get(System.getProperty("java.io.tmpdir"), "ehuman-plugin-classpath");
        Path targetDir = cacheRoot.resolve(key);
        Path marker = targetDir.resolve(".extracted");

        if (Files.isRegularFile(marker)) {
            return targetDir;
        }

        // 清理旧版本留下的缓存，避免反复打包后占满临时目录
        cleanOldExtractedCache(cacheRoot, targetDir);

        Path classesDir = targetDir.resolve("classes");
        Path libDir = targetDir.resolve("lib");
        Files.createDirectories(classesDir);
        Files.createDirectories(libDir);

        try (ZipFile zip = new ZipFile(fatJar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                Path output;
                if (name.startsWith(BOOT_CLASSES_DIR)) {
                    output = classesDir.resolve(name.substring(BOOT_CLASSES_DIR.length()));
                } else if (name.startsWith(BOOT_LIB_DIR) && name.endsWith(".jar")) {
                    output = libDir.resolve(name.substring(BOOT_LIB_DIR.length()));
                } else {
                    continue;
                }

                output = output.normalize();
                if (!output.startsWith(targetDir)) {
                    log.warn("跳过非法路径的 jar 条目: {}", name);
                    continue;
                }

                Files.createDirectories(output.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Files.writeString(marker, "extracted from " + fatJar);
        log.info("Fat Jar 依赖已解压到: {}（首次启动较慢，后续复用缓存）", targetDir);
        return targetDir;
    }

    /**
     * 删除旧版本的解压缓存
     */
    private void cleanOldExtractedCache(Path cacheRoot, Path keepDir) {
        if (!Files.isDirectory(cacheRoot)) {
            return;
        }
        try (Stream<Path> stream = Files.list(cacheRoot)) {
            stream.filter(Files::isDirectory)
                    .filter(dir -> !dir.equals(keepDir))
                    .forEach(dir -> {
                        try (Stream<Path> walk = Files.walk(dir)) {
                            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    log.debug("删除旧缓存失败: {}", path, e);
                                }
                            });
                        } catch (IOException e) {
                            log.debug("清理旧缓存目录失败: {}", dir, e);
                        }
                    });
        } catch (IOException e) {
            log.debug("清理插件编译缓存失败: {}", cacheRoot, e);
        }
    }

    /**
     * 拼接解压目录的 classpath：classes 目录 + 所有依赖 jar
     */
    private String buildExtractedClasspath(Path extractedDir) throws IOException {
        List<String> entries = new ArrayList<>();
        entries.add(extractedDir.resolve("classes").toString());
        try (Stream<Path> stream = Files.list(extractedDir.resolve("lib"))) {
            stream.filter(path -> path.toString().endsWith(".jar"))
                    .sorted()
                    .map(path -> path.toAbsolutePath().toString())
                    .forEach(entries::add);
        }
        return String.join(File.pathSeparator, entries);
    }

    /**
     * 查找解压出来的 lombok，用于注解处理（插件里的 @Slf4j）
     */
    private String findLombokJar(Path extractedDir) {
        Path libDir = extractedDir.resolve("lib");
        try (Stream<Path> stream = Files.list(libDir)) {
            return stream.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("lombok-") && name.endsWith(".jar"))
                    .findFirst()
                    .map(name -> libDir.resolve(name).toAbsolutePath().toString())
                    .orElse(null);
        } catch (IOException e) {
            log.debug("未找到 lombok，使用 javac 默认的注解处理器发现机制", e);
            return null;
        }
    }
    
    /**
     * 加载编译后的类
     */
    private void loadCompiledClasses() throws Exception {
        Path classDir = Paths.get(systemPath.getVoicePluginClassesPath());
        if (!Files.exists(classDir)) {
            return;
        }
        
        // 创建ClassLoader
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{classDir.toUri().toURL()},
            Thread.currentThread().getContextClassLoader()
        );
        
        // 查找所有class文件
        List<Path> classFiles = findClassFiles(classDir);
        
        for (Path classFile : classFiles) {
            try {
                // 获取完整类名
                String className = getClassName(classFile, classDir);
                
                // 加载类
                Class<?> clazz = classLoader.loadClass(className);
                
                // 检查是否实现了TtsPlugin接口
                if (TtsPlugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                    // 创建实例
                    TtsPlugin plugin = (TtsPlugin) clazz.getDeclaredConstructor().newInstance();
                    String pluginName = plugin.getName();
                    
                    // 存储插件
                    plugins.put(pluginName, plugin);
                    
                    // 记录源码路径
                    findSourceFileForClass(className).ifPresent(path -> 
                        pluginSourcePaths.put(pluginName, path)
                    );
                    
                    log.info("✅ 加载插件成功: {} -> {}", pluginName, className);
                } else {
                    log.debug("跳过非插件类: {}", className);
                }
                
            } catch (Exception e) {
                log.error("加载类失败: {}", classFile, e);
            }
        }
    }

    /**
     * 获取所有插件名称
     */
    public List<String> getAllPluginNames() {
        return new ArrayList<>(plugins.keySet());
    }

    
    /**
     * 调用插件方法 - 提交任务
     */
    public Map<String, String> textToSpeech(String pluginName, String text,
                                            String voice,String apiKey,String localPath,
                                            String proxyHost
    ) throws Exception {
        TtsPlugin plugin = getPlugin(pluginName);
        log.info("调用插件 [{}].submitTask: text={}", pluginName, text);
        return plugin.textToSpeech(text, voice,apiKey,localPath,proxyHost);
    }

    /**
     * 获取指定插件
     */
    public TtsPlugin getPlugin(String name) {
        TtsPlugin plugin = plugins.get(name);
        if (plugin == null) {
            throw new IllegalArgumentException("未找到插件: " + name +
                    "，可用插件: " + plugins.keySet());
        }
        return plugin;
    }
    
    /**
     * 查找Java文件
     */
    private List<Path> findJavaFiles(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        if (!Files.exists(dir)) return result;
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                  .forEach(result::add);
        }
        return result;
    }
    
    /**
     * 查找Class文件
     */
    private List<Path> findClassFiles(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        if (!Files.exists(dir)) return result;
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> p.toString().endsWith(".class"))
                  .forEach(result::add);
        }
        return result;
    }
    
    /**
     * 获取类名
     */
    private String getClassName(Path classFile, Path rootDir) {
        String relative = rootDir.relativize(classFile).toString();
        return relative.replace(File.separator, ".")
                       .replace(".class", "");
    }
    
    /**
     * 根据类名查找源文件
     */
    private Optional<Path> findSourceFileForClass(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        Path sourcePath = Paths.get(systemPath.getVoicePluginSourcePath(), simpleName + ".java");
        if (Files.exists(sourcePath)) {
            return Optional.of(sourcePath);
        }
        return Optional.empty();
    }

    /**
     * 清理编译输出目录中的所有 class 文件
     */
    private void cleanClassDirectory() throws IOException {
        Path classDir = Paths.get(systemPath.getVoicePluginClassesPath());
        if (!Files.exists(classDir)) {
            log.info("编译目录不存在，无需清理: {}", classDir);
            return;
        }

        log.info("开始清理编译目录: {}", classDir.toAbsolutePath());

        // 统计删除的文件数量
        long deletedCount = 0;

        try (Stream<Path> stream = Files.walk(classDir)) {
            List<Path> classFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".class"))
                    .collect(Collectors.toList());

            for (Path classFile : classFiles) {
                try {
                    Files.delete(classFile);
                    deletedCount++;
                    log.debug("删除 class 文件: {}", classFile);
                } catch (IOException e) {
                    log.warn("删除文件失败: {}", classFile, e);
                }
            }
        }

        log.info("清理完成，共删除 {} 个 class 文件", deletedCount);
    }

}