
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.util.Iterator;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

class Utilities {


    static PGPPrivateKey retrieveSecretKey(PGPSecretKeyRingCollection krc, long ID, char[] pw)
            throws PGPException, NoSuchProviderException {
        PGPSecretKey krcKey = krc.getSecretKey(ID);

        if (krcKey == null) {
            return null;
        }

        return krcKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pw));
    }


    static PGPPublicKey getPubKey(String file) throws IOException, PGPException {
        InputStream key = new BufferedInputStream(new FileInputStream(file));
        PGPPublicKey publicKey = getPubKey(key);
        key.close();
        return publicKey;
    }


    private static PGPPublicKey getPubKey(InputStream in) throws IOException, PGPException {
        JcaKeyFingerprintCalculator thisCalc = new JcaKeyFingerprintCalculator();
        PGPPublicKeyRingCollection pkrc = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(in), thisCalc);


        Iterator ringIterator = pkrc.getKeyRings();
        while (ringIterator.hasNext()) {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing) ringIterator.next();
            Iterator keyIterator = keyRing.getPublicKeys();
            while (keyIterator.hasNext()) {
                PGPPublicKey key = (PGPPublicKey) keyIterator.next();
                if (key.isEncryptionKey()) {
                    return key;
                }
            }
        }

        throw new IllegalArgumentException("Couldn't find encryption key in key ring.");
    }

    static byte[] compFile(String file, int alg) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PGPCompressedDataGenerator compressed = new PGPCompressedDataGenerator(alg);
        char type = PGPLiteralData.BINARY;
        PGPUtil.writeFileToLiteralData(compressed.open(outputStream), type, new File(file));
        compressed.close();
        return outputStream.toByteArray();
    }

    static PGPSecretKey retrieveSecretKey(String file) throws IOException, PGPException {
        InputStream key = new BufferedInputStream(new FileInputStream(file));
        PGPSecretKey secretKey = retrieveSecretKey(key);
        key.close();
        return secretKey;
    }

    static PGPSecretKey retrieveSecretKey(InputStream input) throws IOException, PGPException {
        PGPSecretKeyRingCollection skrc = new PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator());

        Iterator ringIterator = skrc.getKeyRings();
        while (ringIterator.hasNext()) {
            PGPSecretKeyRing keyRing = (PGPSecretKeyRing) ringIterator.next();

            Iterator keyIterator = keyRing.getSecretKeys();
            while (keyIterator.hasNext()) {
                PGPSecretKey thisKey = (PGPSecretKey) keyIterator.next();

                if (thisKey.isSigningKey()) {
                    return thisKey;
                }
            }
        }

        throw new IllegalArgumentException("Can't find key to sign with.");
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }


}