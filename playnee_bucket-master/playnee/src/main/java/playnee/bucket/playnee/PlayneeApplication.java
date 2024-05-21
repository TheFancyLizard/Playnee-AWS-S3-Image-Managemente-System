package playnee.bucket.playnee;


import com.amazonaws.services.s3.model.AmazonS3Exception;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@OpenAPIDefinition(info = @Info(title = "FileStorage"))
@SpringBootApplication
@Log4j2

public class PlayneeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlayneeApplication.class, args);
	}

	@Bean
	public ApplicationRunner applicationRunner(S3Service s3Service){
		return args -> {
			log.info("Spring Boot AWS S3 integration...");

			try {
				var s3Object = s3Service.getFile("jvm.png");
				log.info(s3Object.getKey());
			} catch (AmazonS3Exception e) {
				log.error(e.getMessage());
			}
		};
	}

}
