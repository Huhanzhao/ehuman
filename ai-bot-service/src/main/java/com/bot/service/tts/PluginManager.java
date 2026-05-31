package com.bot.service.tts;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 插件管理器 - 负责加载、卸载、调用插件
 */
@Component
@Slf4j
public class PluginManager {

    // 配置：插件源码目录
    private String pluginSourceDir = "./plugin/TTS";
    
    // 配置：编译输出目录
    private String pluginClassDir = "./plugin/classes";
    
    // 存储所有已加载的插件
    private final Map<String, TtsPlugin> plugins = new ConcurrentHashMap<>();
    
    // 存储插件源码文件路径
    private final Map<String, Path> pluginSourcePaths = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() throws Exception {
        log.info("========== 插件管理器初始化 ==========");
        log.info("插件源码目录: {}", Paths.get(pluginSourceDir).toAbsolutePath());
        log.info("插件编译目录: {}", Paths.get(pluginClassDir).toAbsolutePath());
        
        // 1. 创建目录
        createDirectories();
        
        // todo 2. 创建示例插件
//        createExamplePlugin();
        
        // 3. 加载所有插件
        loadAllPlugins();

        log.info("====================================");
    }
    
    /**
     * 创建必要的目录
     */
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(pluginSourceDir));
        Files.createDirectories(Paths.get(pluginClassDir));
    }
    
    /**
     * 创建示例插件文件
     */
//    private void createExamplePlugin() throws IOException {
//        Path examplePath = Paths.get(pluginSourceDir, "ExamplePlugin.java");
//        if (Files.exists(examplePath)) {
//            return;
//        }
//
//        String content = """
//package com.user.plugins;
//
//import com.qqrobot.plugin.TtsPlugin;
//
//public class ExamplePlugin implements TtsPlugin {
//
//    @Override
//    public String getName() {
//        return "example";
//    }
//
//    @Override
//    public String submitTask(String text, String voice) throws Exception {
//        System.out.println("【示例插件】提交任务: text=\"%s\", voice=\"%s\"".formatted(text, voice));
//        // TODO: 在这里实现你的提交逻辑
//        return "task_" + System.currentTimeMillis();
//    }
//
//    @Override
//    public String queryStatus(String taskId) throws Exception {
//        System.out.println("【示例插件】查询状态: taskId=\"%s\"".formatted(taskId));
//        // TODO: 在这里实现你的查询逻辑
//        // 返回: "SUCCESS", "PROCESSING", "FAILED"
//        return "SUCCESS";
//    }
//
//    @Override
//    public byte[] getAudio(String taskId) throws Exception {
//        System.out.println("【示例插件】获取音频: taskId=\"%s\"".formatted(taskId));
//        // TODO: 在这里实现你的获取音频逻辑
//        return new byte[0];
//    }
//}
//""";
//        Files.writeString(examplePath, content, StandardCharsets.UTF_8);
//        log.info("已创建示例插件: {}", examplePath.toAbsolutePath());
//    }
    
    /**
     * 加载所有插件
     */
    public void loadAllPlugins() throws Exception {
        log.info("开始加载所有插件...");
        
        // 1. 清空现有插件
        plugins.clear();
        pluginSourcePaths.clear();
        
        // 2. 查找所有Java文件
        List<Path> javaFiles = findJavaFiles(Paths.get(pluginSourceDir));
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
        
        // 转换Path到JavaFileObject
        Iterable<? extends JavaFileObject> compilationUnits = 
            fileManager.getJavaFileObjectsFromFiles(javaFiles.stream()
                .map(Path::toFile)
                .collect(Collectors.toList()));
        
        // 构建classpath
        String classpath = buildClasspath();
        
        List<String> options = Arrays.asList(
            "-d", pluginClassDir,
            "-cp", classpath,
            "-encoding", "UTF-8"
        );
        
        log.debug("编译参数: {}", options);
        
        // 执行编译
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
     * 加载编译后的类
     */
    private void loadCompiledClasses() throws Exception {
        Path classDir = Paths.get(pluginClassDir);
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
    public Map<String, String> textToSpeech(String pluginName, String text, String voice,String apiKey) throws Exception {
        TtsPlugin plugin = getPlugin(pluginName);
        log.info("调用插件 [{}].submitTask: text={}", pluginName, text);
        return plugin.textToSpeech(text, voice,apiKey);
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
     * 构建classpath
     */
    private String buildClasspath() {
        StringBuilder cp = new StringBuilder();
        
        // 当前classpath
        String classpath = System.getProperty("java.class.path");
        cp.append(classpath);
        
        // 添加lib目录下的jar
        Path libDir = Paths.get("./lib");
        if (Files.exists(libDir)) {
            try (Stream<Path> stream = Files.list(libDir)) {
                stream.filter(p -> p.toString().endsWith(".jar"))
                      .forEach(p -> cp.append(File.pathSeparator).append(p.toString()));
            } catch (IOException e) {
                log.warn("读取lib目录失败", e);
            }
        }
        
        return cp.toString();
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
        Path sourcePath = Paths.get(pluginSourceDir, simpleName + ".java");
        if (Files.exists(sourcePath)) {
            return Optional.of(sourcePath);
        }
        return Optional.empty();
    }

}