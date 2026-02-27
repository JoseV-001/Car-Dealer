package br.com.devsuperior.car_dealer.integration;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SftpConfig {

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port:22}")
    private int port;

    @Value("${sftp.user}")
    private String user;

    @Value("${sftp.password}")
    private String password;

    // Cria e autentica um cliente SSH pronto para operacoes SFTP.
    public SSHClient setupSshClient() throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, port);
        client.authPassword(user, password);
        return client;
    }
}
