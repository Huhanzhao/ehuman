package com.helper.services.service.impl;

import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.service.Upload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class UploadTleFileServiceImpl implements Upload {
    @Override
    public String uploadResourceFile(MultipartFile file) {
        Path tempFile = null;
        Path backupDir = null;

        try {
            if (file == null || file.isEmpty()) {
                return "文件为空";
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return "文件名不能为空";
            }

            // 源文件名转小写
            String lowerName = originalFilename.toLowerCase();
            if (!(lowerName.endsWith(".zip") || lowerName.endsWith(".7z") || lowerName.endsWith(".tar"))) {
                return "仅支持上传 .zip、.7z、.tar 格式的资源文件";
            }
            log.info("小写之后的文件压缩包名{}",lowerName);

            // 获取压缩包后缀
            String suffix = lowerName.substring(lowerName.lastIndexOf('.'));
            log.info("后缀名{}",suffix);

            // 文件写出到临时文件
            tempFile = Files.createTempFile("resource-upload-", suffix);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }


            // 没有apps目录就创建
            Path appsRoot = Paths.get("/apps");
            if (!Files.exists(appsRoot)) {
                Files.createDirectories(appsRoot);
            }

            // 有resource文件夹就备份一下老文件
            Path resourceDir = appsRoot.resolve("resource");
            long l = System.currentTimeMillis();
            if (Files.exists(resourceDir)) {
                backupDir = appsRoot.resolve("resource_backup_" + l);
                Files.move(resourceDir, backupDir, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("文件备份完毕{}","resource_backup_" + l);

            // 创建一个新的resource文件夹
            Files.createDirectories(resourceDir);

//             解压压缩包到新的文件夹里面
            extractArchive(tempFile, resourceDir, suffix);

//            // 判断是否压缩包里面有resource文件夹 apps/resource/resource 存在就移动到正确的位置
            flattenResourceRoot(resourceDir);

            // 判断文件是否合法
            judgeFiles(tempFile, resourceDir, suffix);

            // 备份文件存在就删除
            if (backupDir != null && Files.exists(backupDir)) {
                deleteDirectory(backupDir);
            }
            return "资源文件已更新";
        }catch (BusinessException ee){
            try {
                // 回滚文件
                Path appsRoot = Paths.get("/apps");
                Path resourceDir = appsRoot.resolve("resource");
                if (Files.exists(resourceDir)) {
                    deleteDirectory(resourceDir);
                }
                if (backupDir != null && Files.exists(backupDir)) {
                    Files.move(backupDir, resourceDir, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("资源回滚成功");
            } catch (Exception rollbackEx) {
                log.error("资源文件回滚失败", rollbackEx);
            }
            throw new BusinessException(ee.getMessage());
        }
        catch (Exception e) {
            log.error("上传资源文件失败", e);
            try {
                // 回滚文件
                Path appsRoot = Paths.get("/apps");
                Path resourceDir = appsRoot.resolve("resource");
                if (Files.exists(resourceDir)) {
                    deleteDirectory(resourceDir);
                }
                if (backupDir != null && Files.exists(backupDir)) {
                    Files.move(backupDir, resourceDir, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("资源回滚成功");
            } catch (Exception rollbackEx) {
                log.error("资源文件回滚失败", rollbackEx);
            }
            return "文件上传失败，请检查文件内容";
        } finally {
            // 删除临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ex) {
                    log.warn("删除临时文件失败: {}", tempFile, ex);
                }
            }
        }
    }

    private void flattenResourceRoot(Path resourceDir) throws Exception {
        // 判断/apps/resource/resource是否存在
        Path nestedResourceDir = resourceDir.resolve("resource");
        if (!Files.isDirectory(nestedResourceDir)) {
            return;
        }

        // 存在就把resource文件夹下的文件移动到/apps/resource里面
        // 获取resource文件夹下面所有文件
        List<Path> children;
        try (Stream<Path> stream = Files.list(nestedResourceDir)) {
            children = stream.collect(Collectors.toList());
        }

        // 遍历每一个文件
        for (Path child : children) {
            Path fileName = child.getFileName();
            Path target = resourceDir.resolve(fileName);

            // 如果目标位置存在就删除
            if (Files.exists(target)) {
                if (Files.isDirectory(target)) {
                    deleteDirectory(target);
                } else {
                    Files.delete(target);
                }
            }

            // 移动到目标位置
            Files.move(child, target, StandardCopyOption.REPLACE_EXISTING);
        }
        // 删除/apps/resource/resource
        deleteDirectory(nestedResourceDir);
    }

    /**
     * 根据后缀调用系统命令解压压缩包
     */
    private void extractArchive(Path archive, Path targetDir, String suffix) throws BusinessException {
        List<String> command;
        if (".zip".equals(suffix)) {
            command = List.of("unzip", "-o", archive.toString(), "-d", targetDir.toString());
        } else if (".tar".equals(suffix)) {
            command = List.of("tar", "-xf", archive.toString(), "-C", targetDir.toString());
        } else if (".7z".equals(suffix)) {
            command = List.of("7z", "x", archive.toString(), "-o" + targetDir.toString(), "-y");
        } else {
            throw new IllegalArgumentException("不支持的文件格式");
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = null;
        try {
            process = pb.start();
            log.info("开始解压压缩包到{},执行命令{}",targetDir,command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exitCode != 0) {
            throw new RuntimeException("解压失败，exitCode=" + exitCode);
        }
        log.info("解压完毕退出码{}",exitCode);
        //给权限
        setPermissionsRecursively(targetDir);
    }


    public void judgeFiles(Path archive, Path targetDir, String suffix){
        String resouceFiles = "aaa.txt&&bbb.txt&&IAU2006";
        String iauFiles = "111.txt&&222.txt";
        log.info("开始判断文件合法");
        List<String> resouceFilesList = new ArrayList<>(Arrays.asList(resouceFiles.split("&&")));
        log.info("获取reource文件列表成功{}",resouceFilesList.toString());
        List<String> iauFilesList = new ArrayList<>(Arrays.asList(iauFiles.split("&&")));
        log.info("获取iau文件列表成功{}",iauFilesList.toString());


        List<Path> children;
        try (Stream<Path> stream = Files.list(targetDir)) {
            children = stream.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("resource文件夹下面的文件{}",children);

        List<Path> iauChildren;
        Path iau2006 = targetDir.resolve("IAU2006");
        if (!Files.exists(iau2006)) {
            throw new BusinessException("文件上传失败：缺少IAU2006文件夹");
        }
        try (Stream<Path> stream = Files.list(iau2006)) {
            iauChildren = stream.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("IAU2006文件夹下面的文件{}",children);

        // 判断文件是否合法
        // resource 下的文件和文件夹
        for (Path child : children) {
            boolean exist = false;
            String fileName = child.getFileName().toString().replace("/apps/resource/", "");
            log.info("开始判断resource 下文件{}是否合法",fileName);
            if(resouceFilesList.isEmpty()){
                // 所有文件都有了还在循环说明文件上传多了
                throw new BusinessException("文件上传失败：无法识别文件" + fileName);
            }
            for (int i = 0; i < resouceFilesList.size(); i++) {
                String name = resouceFilesList.get(i);
                if (name.equals(fileName)){
                    resouceFilesList.remove(name);
                    exist = true;
                }
            }

            // 没有这个文件
            if (!exist){
                throw new BusinessException("文件上传失败：无法识别文件" + fileName);
            }
        }
        // 遍历删除之后还有文件 缺少
        if (!resouceFilesList.isEmpty()){
            throw new BusinessException("文件上传失败：缺少文件或文件夹" + resouceFilesList.getFirst());
        }

        for (Path child : iauChildren){
            boolean exist = false;
            String fileName = child.getFileName().toString().replace("/apps/resource/IAU2006", "");
            log.info("开始判断iau 下文件{}是否合法",fileName);
            if(iauFilesList.isEmpty()){
                // 所有文件都有了还在循环说明文件上传多了
                throw new BusinessException("文件上传失败：无法识别文件" + fileName);
            }
            for (int i = 0; i < iauFilesList.size(); i++) {
                String iauName = iauFilesList.get(i);
                if (iauName.equals(fileName)){
                    iauFilesList.remove(iauName);
                    exist = true;
                }
            }

            // 没有这个文件
            if (!exist){
                throw new BusinessException("文件上传失败：无法识别文件" + fileName);
            }
        }
        // 遍历删除之后还有文件 缺少
        if (!iauFilesList.isEmpty()){
            throw new BusinessException("文件上传失败：缺少文件或文件夹" + iauFilesList.getFirst());
        }
    }

    /**
     * 递归删除目录及其下所有内容
     */
    private void deleteDirectory(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception e) {
                        log.warn("删除路径失败: {}", p, e);
                    }
                });
    }

    private static final Set<PosixFilePermission> PERMISSIONS_775 =
            PosixFilePermissions.fromString("rwxrwxr-x");

    private void setPermissionsRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            FileStore fileStore = Files.getFileStore(path);
            if (!fileStore.supportsFileAttributeView(PosixFileAttributeView.class)) {
                log.warn("文件系统不支持POSIX权限设置，路径: {}", path);
                return;
            }
        } catch (IOException e) {
            log.warn("检测POSIX支持失败，路径: {}", path, e);
            return;
        }
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.setPosixFilePermissions(p, PERMISSIONS_775);
                        } catch (IOException | UnsupportedOperationException ex) {
                            log.warn("设置权限失败: {}", p, ex);
                        }
                    });
        } catch (IOException e) {
            log.warn("遍历路径设置权限失败: {}", path, e);
        }
    }
}
