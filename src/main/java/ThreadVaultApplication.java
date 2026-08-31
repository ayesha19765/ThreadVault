import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot entry point for ThreadVault Web & REST API layer.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "controller",
        "service",
        "config",
        "backup",
        "restore",
        "metadata",
        "stats",
        "dedup",
        "compression",
        "incremental",
        "scanner",
        "scheduler",
        "watcher",
        "cli"
})
public class ThreadVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreadVaultApplication.class, args);
    }
}
