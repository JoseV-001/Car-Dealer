package br.com.devsuperior.car_dealer.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SftpDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpDownloadService.class);

    private final SftpConfig sftpConfig;

    @Value("${sftp.dir.remote.download}")
    private String remoteDownloadDir;

    @Value("${sftp.dir.local.download}")
    private String localDownloadDir;

    // Injeta a configuracao usada para abrir conexao SFTP.
    public SftpDownloadService(SftpConfig sftpConfig) {
        this.sftpConfig = sftpConfig;
    }

    // Baixa arquivos novos do diretorio remoto para o diretorio local.
    public int downloadNewFiles() {
        Path localDir = Path.of(localDownloadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(localDir);
            clearLocalDownloadDirectory(localDir);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel preparar pasta local de download: " + localDir, e);
        }

        int downloadedFilesCount = 0;

        try (SSHClient sshClient = sftpConfig.setupSshClient();
                SFTPClient sftpClient = sshClient.newSFTPClient()) {

            List<RemoteResourceInfo> files = sftpClient.ls(remoteDownloadDir);
            for (RemoteResourceInfo file : files) {
                if (file.isRegularFile()) {
                    File localFile = localDir.resolve(file.getName()).toFile();
                    String remoteFilePath = remoteDownloadDir + "/" + file.getName();
                    sftpClient.get(remoteFilePath, new FileSystemFile(localFile));
                    downloadedFilesCount++;
                    LOGGER.info("Arquivo baixado do SFTP: {}", file.getName());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro na rotina de download SFTP", e);
        }

        return downloadedFilesCount;
    }

    // Remove arquivos locais antigos antes de uma nova rodada de download.
    private void clearLocalDownloadDirectory(Path localDir) throws IOException {
        List<Path> filesToDelete = new ArrayList<>();
        try (var paths = Files.list(localDir)) {
            paths.filter(Files::isRegularFile).forEach(filesToDelete::add);
        }

        for (Path filePath : filesToDelete) {
            Files.delete(filePath);
        }
    }
}
