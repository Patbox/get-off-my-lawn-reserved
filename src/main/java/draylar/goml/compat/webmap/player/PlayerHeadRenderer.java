package draylar.goml.compat.webmap.player;

import draylar.goml.GetOffMyLawn;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public final class PlayerHeadRenderer {

    private static final Map<String, String> HEAD_IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final int SKIN_REQUEST_TIMEOUT_MS = 5000;
    private static final Gson GSON = new Gson();

    private PlayerHeadRenderer() {}

    /**
     * Renders the head portion of a skin texture from a URL and encodes it as a base64 png string.
     * 
     * @param textureUrl the URL of the skin texture
     * @return an optional containing the base64-encoded png string if successful, or an empty optional if failed
     */
    public static Optional<String> headImageFromSkinUrl(@Nullable String textureUrl) {
        try {
            if (textureUrl == null || textureUrl.isBlank()) {
                return Optional.empty();
            }

            if (HEAD_IMAGE_CACHE.containsKey(textureUrl)) {
                return Optional.of(HEAD_IMAGE_CACHE.get(textureUrl));
            }

            BufferedImage skinTexture = loadSkinTexture(textureUrl);
            if (skinTexture == null) {
                return Optional.empty();
            }

            BufferedImage renderedHead = renderHead(skinTexture);
            String encodedImage = encodeImage(renderedHead);
            if (encodedImage.isEmpty()) {
                return Optional.empty();
            }

            HEAD_IMAGE_CACHE.put(textureUrl, encodedImage);
            return Optional.of(encodedImage);
        } catch (IOException exception) {
            GetOffMyLawn.LOGGER.warn("Failed to download skin texture from {}: {}", textureUrl, exception.getMessage());
            return Optional.empty();
        } catch (RuntimeException exception) {
            GetOffMyLawn.LOGGER.warn("Failed to render head texture for {}: {}", textureUrl, exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extracts the skin URL from a base64-encoded skin value.
     *
     * @param skinValue the base64-encoded skin value
     * @return the skin URL, or null if it could not be extracted
     */
    public static @Nullable String getUrlFromSkinValue(String skinValue) {
		try {
			JsonObject root = GSON.fromJson(new String(Base64.getDecoder().decode(skinValue)), JsonObject.class);
			if (root == null || !root.has("textures")) {
				return null;
			}

			JsonObject textures = root.getAsJsonObject("textures");
			if (textures == null || !textures.has("SKIN")) {
				return null;
			}

			JsonObject skin = textures.getAsJsonObject("SKIN");
			if (skin == null || !skin.has("url")) {
				return null;
			}

			return skin.getAsJsonPrimitive("url").getAsString();
		} catch (IllegalArgumentException | JsonParseException exception) {
			return null;
		}
    }

    /**
     * Loads a skin texture from a URL.
     * 
     * @param src skin texture URL
     * @return bufferedImage containing the skin texture
     * @throws IOException if the texture could not be loaded
     */
    public static BufferedImage loadSkinTexture(String src) throws IOException {
        URI textureUri;
        try {
            textureUri = URI.create(src);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid skin texture URL: " + src, exception);
        }

        URLConnection connection = textureUri.toURL().openConnection();
        connection.setConnectTimeout(SKIN_REQUEST_TIMEOUT_MS);
        connection.setReadTimeout(SKIN_REQUEST_TIMEOUT_MS);

        try (InputStream inputStream = connection.getInputStream()) {
            return ImageIO.read(inputStream);
        }
    }

    /**
     * Encodes a BufferedImage as a base64 png string.
     * 
     * @param image the image to encode
     * @return a base64-encoded png string
     * @throws IOException if the image could not be encoded
     */
    public static String encodeImage(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", outputStream)) {
                return "";
            }

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    /*
    * Adapted from de.bluecolored.bluemap.common.plugin.skins.DefaultPlayerIconFactory:
    *
    * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
    * Copyright (c) contributors
    *
    * Permission is hereby granted, free of charge, to any person obtaining a copy
    * of this software and associated documentation files (the "Software"), to deal
    * in the Software without restriction, including without limitation the rights
    * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    * copies of the Software, and to permit persons to whom the Software is
    * furnished to do so, subject to the following conditions:
    *
    * The above copyright notice and this permission notice shall be included in
    * all copies or substantial portions of the Software.
    *
    * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    * THE SOFTWARE.
    */

    /**
     * Renders the head portion of a player skin texture by extracting and combining skin layers.
     * 
     * @param skin the full skin texture image
     * @return the rendered head image, or a fallback 8x8 image if rendering fails
     */
    public static BufferedImage renderHead(BufferedImage skin) {
        BufferedImage head;

        BufferedImage layer1 = skin.getSubimage(8, 8, 8, 8);
        BufferedImage layer2 = skin.getSubimage(40, 8, 8, 8);

        try {
            head = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = head.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(layer1, 4, 4, 40, 40, null);
            graphics.drawImage(layer2, 0, 0, 48, 48, null);
        } catch (Throwable throwable) {
            GetOffMyLawn.LOGGER.warn("Could not access Graphics2D to render player-skin texture. Try adding '-Djava.awt.headless=true' to your startup flags or ignore this warning.");
            head = new BufferedImage(8, 8, skin.getType());
            layer1.copyData(head.getRaster());
        }

        return head;
    }
}