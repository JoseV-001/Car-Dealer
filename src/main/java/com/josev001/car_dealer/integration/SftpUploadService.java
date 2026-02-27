package br.com.devsuperior.car_dealer.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SftpUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpUploadService.class);
    private static final DateTimeFormatter FILE_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final SftpConfig sftpConfig;

    @Value("${sftp.dir.local.upload}")
    private String localUploadDir;

    @Value("${sftp.dir.remote.upload}")
    private String remoteUploadDir;

    @Value("${app.matriz-report-file}")
    private String matrizReportFile;

    @Value("${sftp.dir.remote.download}")
    private String remoteDownloadDir;

    @Value("${sftp.dir.remote.processed}")
    private String remoteProcessedDir;

    @Value("${sftp.dir.local.download}")
    private String localDownloadDir;

    // Injeta a configuracao usada para abrir conexao SFTP.
    public SftpUploadService(SftpConfig sftpConfig) {
        this.sftpConfig = sftpConfig;
    }

    // Envia o relatorio da matriz e move arquivos processados para o destino final.
    public void uploadAndMoveProcessedFiles() {
        Path uploadDir = Path.of(localUploadDir).toAbsolutePath().normalize();
        Path matrizReportPath = Path.of(matrizReportFile).toAbsolutePath().normalize();
        Path localFiliaisDir = Path.of(localDownloadDir).toAbsolutePath().normalize();
        Path localProcessedDir = localFiliaisDir.resolve("processados");
        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(localFiliaisDir);
            Files.createDirectories(localProcessedDir);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel preparar diretorios locais SFTP", e);
        }

        try (SSHClient sshClient = sftpConfig.setupSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {
            ensureRemoteDirectoryExists(sftpClient, remoteProcessedDir);

            uploadSingleMatrizFile(sftpClient, uploadDir, matrizReportPath);

            List<RemoteResourceInfo> remoteFiles = sftpClient.ls(remoteDownloadDir);
            for (RemoteResourceInfo remoteFile : remoteFiles) {
                if (remoteFile.isRegularFile()) {
                    processFileToProcessed(sftpClient, remoteFile.getName(), localFiliaisDir, localProcessedDir);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro na rotina de upload/movimentacao SFTP", e);
        }
    }

    // Faz upload do arquivo mais recente da matriz e remove a copia local.
    private void uploadSingleMatrizFile(SFTPClient sftpClient, Path uploadDir, Path matrizReportPath) throws IOException {
        Optional<Path> matrizFilePath = resolveMatrizFileToUpload(uploadDir, matrizReportPath);
        if (matrizFilePath.isEmpty()) {
            LOGGER.info("Arquivo da matriz nao encontrado para upload em {}", uploadDir);
            return;
        }

        File matrizFile = matrizFilePath.get().toFile();
        String remoteTargetPath = remoteUploadDir + "/" + matrizFile.getName();
        sftpClient.put(new FileSystemFile(matrizFile), remoteTargetPath);
        LOGGER.info("Arquivo de matriz enviado: {}", matrizFile.getName());

        if (!matrizFile.delete()) {
            LOGGER.warn("Nao foi possivel remover arquivo local apos upload: {}", matrizFile.getAbsolutePath());
        }
    }

    // Localiza o arquivo da matriz mais recente com base no nome configurado.
    private Optional<Path> resolveMatrizFileToUpload(Path uploadDir, Path matrizReportPath) throws IOException {
        String configuredFileName = matrizReportPath.getFileName().toString();
        int dotIndex = configuredFileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? configuredFileName.substring(0, dotIndex) : configuredFileName;
        String extension = dotIndex > 0 ? configuredFileName.substring(dotIndex) : "";
        String prefix = baseName + "_";

        try (var paths = Files.list(uploadDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(extension);
                    })
                    .max(Comparator.comparing(path -> path.toFile().lastModified()));
        }
    }

    // Garante a existencia do diretorio remoto para receber arquivos processados.
    private void ensureRemoteDirectoryExists(SFTPClient sftpClient, String remoteDir) throws IOException {
        if (sftpClient.statExistence(remoteDir) == null) {
            sftpClient.mkdirs(remoteDir);
        }
    }

    // Move um arquivo da pasta remota de entrada para a pasta de processados.
    private void processFileToProcessed(SFTPClient sftpClient, String originalFileName, Path localFiliaisDir,
                                        Path localProcessedDir) throws IOException {
        String renamedFileName = addTimestampSuffix(originalFileName);
        String remoteSourcePath = remoteDownloadDir + "/" + originalFileName;
        String remoteProcessedPath = remoteProcessedDir + "/" + renamedFileName;
        Path localProcessedPath = localProcessedDir.resolve(renamedFileName);

        sftpClient.get(remoteSourcePath, new FileSystemFile(localProcessedPath.toFile()));
        sftpClient.put(new FileSystemFile(localProcessedPath.toFile()), remoteProcessedPath);
        sftpClient.rm(remoteSourcePath);

        Path localFilialFile = localFiliaisDir.resolve(originalFileName);
        if (Files.exists(localFilialFile)) {
            Files.delete(localFilialFile);
        }

        LOGGER.info("Arquivo processado para processados: {} -> {}", originalFileName, renamedFileName);
    }

    // Adiciona timestamp ao nome do arquivo preservando a extensao quando existir.
    private String addTimestampSuffix(String fileName) {
        String timestamp = LocalDateTime.now().format(FILE_SUFFIX_FORMATTER);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String baseName = fileName.substring(0, dotIndex);
            String extension = fileName.substring(dotIndex);
            return baseName + "_" + timestamp + extension;
        }
        return fileName + "_" + timestamp;
    }
}
