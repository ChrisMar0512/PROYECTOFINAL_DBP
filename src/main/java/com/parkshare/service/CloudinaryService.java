package com.parkshare.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Servicio para gestión de imágenes en Cloudinary.
 *
 * Todas las fotos de cocheras se almacenan en la carpeta
 * 'parkshare/parking-spaces' dentro de la cuenta de Cloudinary.
 * El publicId retornado se persiste en la entidad ParkingSpace
 * para poder eliminar la imagen si el host actualiza o elimina la cochera.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Sube una imagen a Cloudinary y retorna la URL segura (HTTPS).
     *
     * @param file archivo de imagen recibido desde el endpoint
     * @return URL segura de la imagen subida
     * @throws RuntimeException si ocurre un error al leer el archivo o al subir
     */
    public String uploadImage(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "parkshare/parking-spaces"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            log.info("Imagen subida a Cloudinary exitosamente: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Error al leer el archivo de imagen para subir a Cloudinary", e);
            throw new RuntimeException(
                    "No se pudo leer el archivo de imagen: " + e.getMessage(), e
            );
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new RuntimeException(
                    "Error al subir la imagen a Cloudinary: " + e.getMessage(), e
            );
        }
    }

    /**
     * Elimina una imagen de Cloudinary usando su publicId.
     * Se llama cuando el host actualiza la foto de su cochera o elimina el espacio.
     *
     * @param publicId el identificador público de la imagen en Cloudinary
     */
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            log.warn("Se intentó eliminar una imagen con publicId nulo o vacío — omitiendo.");
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Imagen eliminada de Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Error al eliminar imagen de Cloudinary con publicId: {}", publicId, e);
            throw new RuntimeException(
                    "No se pudo eliminar la imagen de Cloudinary: " + e.getMessage(), e
            );
        }
    }

    /**
     * Extrae el publicId del resultado de subida.
     * El publicId se necesita para eliminar la imagen en el futuro.
     *
     * @param file archivo a subir
     * @return array con [secureUrl, publicId]
     */
    public String[] uploadImageWithPublicId(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "parkshare/parking-spaces"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId  = (String) result.get("public_id");

            log.info("Imagen subida a Cloudinary: url={}, publicId={}", secureUrl, publicId);
            return new String[]{secureUrl, publicId};

        } catch (IOException e) {
            log.error("Error al leer el archivo de imagen", e);
            throw new RuntimeException(
                    "No se pudo leer el archivo de imagen: " + e.getMessage(), e
            );
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new RuntimeException(
                    "Error al subir la imagen a Cloudinary: " + e.getMessage(), e
            );
        }
    }
}
