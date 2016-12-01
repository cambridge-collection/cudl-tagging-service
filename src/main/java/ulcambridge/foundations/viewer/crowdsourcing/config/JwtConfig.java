package ulcambridge.foundations.viewer.crowdsourcing.config;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.impl.DefaultJwtParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.function.Consumer;

@Configuration
public class JwtConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public JwtParser jwtParser(
        @Qualifier("jwtKeyAssigner") Consumer<JwtParser> keyAssigner,
        @Qualifier("jwtAudience") String audience) {
        JwtParser p = new DefaultJwtParser();

        keyAssigner.accept(p);
        p.requireAudience(audience);

        return p;
    }

    @Bean
    public String jwtAudience(
        @Value("${cudl.tagging.jwt.audience}") String audience) {

        return audience;
    }

    @Bean(name="jwtKeyBytes")
    @ConditionalOnProperty(name = "cudl.tagging.jwt.key.location")
    public byte[] jwtKeyBytesResource(
        @Value("${cudl.tagging.jwt.key.location}") String resourcePath,
        ResourceLoader resourceLoader) throws IOException {

        return StreamUtils.copyToByteArray(
            resourceLoader.getResource(resourcePath).getInputStream());
    }

    @Bean(name="jwtKeyBytes")
    @ConditionalOnProperty(name = "cudl.tagging.jwt.key.value")
    public byte[] jwtKeyBytesValue(
        @Value("${cudl.tagging.jwt.key.value}") String value,
        @Value("${cudl.tagging.jwt.key.value-encoding:UTF-8}") String encoding)
        throws UnsupportedEncodingException {

        if("base64".equalsIgnoreCase(encoding)) {
            return Base64.getDecoder().decode(value.replaceAll("\\s", ""));
        }

        return value.getBytes(value);
    }

    @Configuration
    @ConditionalOnProperty(name = "cudl.tagging.jwt.key.type",
                           havingValue = "public")
    public class JwtPublicKeyConfig {
        @Bean
        public Consumer<JwtParser> jwtKeyAssigner(
            @Qualifier("jwtPublicKey") PublicKey key) {

            return parser -> parser.setSigningKey(key);
        }

        /**
         * The public key should be DER encoded, e.g:
         *
         * <pre>{@code
         * $ openssl rsa -in my-priv-key -outform DER -pubout > my-pub-key
         * }</pre>
         */
        @Bean
        public PublicKey jwtPublicKey(
            @Qualifier("jwtKeyBytes") byte[] key)
            throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidKeySpecException {

            return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(key));
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "cudl.tagging.jwt.key.type",
                           havingValue = "shared",
                           matchIfMissing = true)
    public class JwtSharedSecretKeyConfig {

        public Consumer<JwtParser> jwtKeyAssigner(
            @Qualifier("jwtKeyBytes") byte[] key) {

            return parser -> parser.setSigningKey(key);
        }


    }
}
