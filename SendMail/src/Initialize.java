import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileOutputStream;
import java.io.IOException;
import org.bouncycastle.openpgp.operator.jcajce.*;
import java.io.OutputStream;
import org.bouncycastle.openpgp.*;
import java.security.*;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import java.util.Date;
import java.util.Scanner;

//Loading keys to program or creating
public class Initialize {
    public static void main(String[] args) throws java.lang.Exception {
        Security.addProvider(new BouncyCastleProvider());

        Scanner sc = new Scanner(System.in);
        System.out.println("If you have existing public and secret keys, press 1 to load them in");
        System.out.println("Otherwise press 2 to create new keys");
        System.out.println("Note if you create new keys, your default identity is R, password is C");
        int choice = sc.nextInt();
        if (choice == 1) {
            PGPKeyPair thisPair = new PGPKeyPair(Utilities.getPubKey("keys/pub.asc"), Utilities.retrieveSecretKey("keys/secret.asc").extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(SendEmail.pass)));
            KeyringManager.createKeyRingCollection(thisPair, "R", "C", "keys/PubKeyCollection");
        } else {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

            kpg.initialize(1024);

            KeyPair kp = kpg.generateKeyPair();

            FileOutputStream out1 = new FileOutputStream("keys/secret.asc");
            FileOutputStream out2 = new FileOutputStream("keys/pub.asc");

            createKeyPair(out1, out2, kp, "R", SendEmail.pass);

            PGPKeyPair thisPair = new PGPKeyPair(Utilities.getPubKey("keys/pub.asc"), Utilities.retrieveSecretKey("keys/secret.asc").extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(SendEmail.pass)));
            KeyringManager.createKeyRingCollection(thisPair, "R", "C", "keys/PubKeyCollection");
        }

    }

    private static void createKeyPair(
            OutputStream secretKeyStream,
            OutputStream publicKeyStream,
            KeyPair keyPairIn,
            String name,
            char[] passPhrase)
            throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException {
        secretKeyStream = new ArmoredOutputStream(secretKeyStream);

        PGPDigestCalculator thisCalc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyPair thisPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, keyPairIn, new Date());
        int sigCert = PGPSignature.DEFAULT_CERTIFICATION;
        PGPSignatureSubpacketVector v1 = null;
        PGPSignatureSubpacketVector v2 = null;
        JcaPGPContentSignerBuilder thisCSBuilder = new JcaPGPContentSignerBuilder(thisPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);
        PBESecretKeyEncryptor thisEncryptor = new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, thisCalc).setProvider("BC").build(passPhrase);
        PGPSecretKey thisKey = new PGPSecretKey(sigCert, thisPair, name, thisCalc, v1, v2,thisCSBuilder ,thisEncryptor );

        thisKey.encode(secretKeyStream);

        secretKeyStream.close();
        publicKeyStream = new ArmoredOutputStream(publicKeyStream);
        PGPPublicKey key = thisKey.getPublicKey();
        key.encode(publicKeyStream);
        publicKeyStream.close();
    }
}