package playnee.bucket.playnee;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping
    public String health() {
        return "UP";
    }

    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        s3Service.uploadFile(file.getOriginalFilename(), file);
        return "File uploaded";
    }

    @GetMapping("/download/{fileName}")
    public void downloadFile(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        // Verifica se o arquivo está na pasta "imagem_cache"
        File cachedFile = new File("imagem_cache/" + fileName);
        if (cachedFile.exists()) {
            // Se o arquivo estiver na cache, serve o arquivo a partir da cache
            try (var inputStream = new java.io.FileInputStream(cachedFile);
                 var outputStream = response.getOutputStream()) {
                response.setContentType("image/jpg");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } else {
            // Se o arquivo não estiver na cache, busca do S3 e salva na cache
            S3Object arquivo = s3Service.getFile(fileName);
            S3ObjectInputStream arquivo_imagem = arquivo.getObjectContent();

            // Cria a pasta "imagem_cache" se não existir
            File cacheDir = new File("imagem_cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            // Salva o arquivo na pasta "imagem_cache"
            try (var fileOutputStream = new FileOutputStream(cachedFile);
                 var responseOutputStream = response.getOutputStream()) {
                response.setContentType("image/jpg");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = arquivo_imagem.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);  // Salva no arquivo
                    responseOutputStream.write(buffer, 0, bytesRead);  // Envia a resposta
                }
            } finally {
                arquivo_imagem.close();
            }
        }
    }

    @GetMapping("/view/{fileName}")
    public ResponseEntity<InputStreamResource> viewFile(@PathVariable String fileName) {
        var s3Object = s3Service.getFile(fileName);
        var content = s3Object.getObjectContent();
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(new InputStreamResource(content));
        } catch (Exception e) {
            try {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(new InputStreamResource(content));
            } catch (Exception r) {
                return null;
            }
        }
    }
}

