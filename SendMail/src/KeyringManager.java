import org.bouncycastle.openpgp.*;
import java.io.*;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import java.security.*;

import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.jcajce.*;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import java.util.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class KeyringManager {

    public static void createKeyRingCollection(PGPKeyPair in, String id, String pass, String name) throws java.io.IOException, org.bouncycastle.openpgp.PGPException, InterruptedException {

        char[] givenPW = pass.toCharArray();
        PGPDigestCalculator thisCalc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        int sigCert = PGPSignature.DEFAULT_CERTIFICATION;
        PGPSignatureSubpacketVector v1 = null;
        PGPSignatureSubpacketVector v2 = null;
        JcaPGPContentSignerBuilder thisCSBuilder = new JcaPGPContentSignerBuilder(in.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);
        int cast = PGPEncryptedData.CAST5;
        PBESecretKeyEncryptor thisEncryptor = new JcePBESecretKeyEncryptorBuilder(cast, thisCalc).setProvider("BC").build(givenPW);
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(sigCert, in, id, thisCalc, v1, v2, thisCSBuilder, thisEncryptor);


        PGPPublicKeyRing pubRing = keyRingGen.generatePublicKeyRing();
        OutputStream out1 = new FileOutputStream( "keys/FirstKey.pkr");
        out1 = new ArmoredOutputStream(out1);
        pubRing.encode(out1);


        out1.close();

        out1 = new FileOutputStream(name + ".pkr");
        InputStream inStream = new FileInputStream("keys/FirstKey.pkr");
        PGPPublicKeyRingCollection thisCollection = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(inStream), new JcaKeyFingerprintCalculator());
        out1 = new ArmoredOutputStream(out1);

        thisCollection.encode(out1);


        out1.close();

    }
    public static void main(String args[]) throws IOException, PGPException{
        Security.addProvider(new BouncyCastleProvider());
        Scanner sc = new Scanner(System.in);
        System.out.println("Type a to add another person's public key to your key ring");
        System.out.println("Type r to remove another person's public key from your key ring");
        System.out.println("Type l to list identities currently stored");

        char thisChar = sc.next().charAt(0);
        if(thisChar==('a')){



            System.out.println("Type the ID (name) of the person whose key you wish to add (one word)");
            String addID = sc.next();
            System.out.println("Type the full filename of their key");
            String filename = sc.next();
            InputStream inStream = new FileInputStream("PubKeyCollection.pkr");
            PGPPublicKeyRingCollection thisCollection = new PGPPublicKeyRingCollection(
                    PGPUtil.getDecoderStream(inStream), new JcaKeyFingerprintCalculator());
            PGPPrivateKey thisPriv = Utilities.retrieveSecretKey("secret.asc").extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(SendEmail.pass));
            PGPKeyPair thisPair = new PGPKeyPair(Utilities.getPubKey(filename),thisPriv);
            PGPDigestCalculator thisCalc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
            JcaPGPContentSignerBuilder thisCSBuilder =  new JcaPGPContentSignerBuilder(thisPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);
            PBESecretKeyEncryptor thisEncryptor = new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, thisCalc).setProvider("BC").build(SendEmail.pass);
            int sig = PGPSignature.DEFAULT_CERTIFICATION;
            PGPSignatureSubpacketVector v1 = null;
            PGPSignatureSubpacketVector v2 = null;
            PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(sig, thisPair, addID, thisCalc, v1, v2,thisCSBuilder ,thisEncryptor);
            thisCollection = PGPPublicKeyRingCollection.addPublicKeyRing(thisCollection,keyRingGen.generatePublicKeyRing());
            OutputStream out1 = new FileOutputStream("PubKeyCollection.pkr");
            out1 = new ArmoredOutputStream(out1);

            thisCollection.encode(out1);
            out1.close();

            File f = new File("Identities.txt");
            f.createNewFile();
            String current = Utilities.readFile("Identities.txt",StandardCharsets.US_ASCII);
            current = current.concat(addID);
            PrintWriter out = new PrintWriter("Identities.txt");
            out.println(current);
            out.close();

        }else if(thisChar==('r')){
            System.out.println("Type the ID of the person whose key you wish to remove");
            String remove = sc.next().trim();
            InputStream inStream = new FileInputStream("PubKeyCollection.pkr");
            PGPPublicKeyRingCollection thisCollection = new PGPPublicKeyRingCollection(
                    PGPUtil.getDecoderStream(inStream), new JcaKeyFingerprintCalculator());
            Iterator<PGPPublicKeyRing> thisList = thisCollection.getKeyRings(remove);
            thisCollection = PGPPublicKeyRingCollection.removePublicKeyRing(thisCollection,thisList.next());
            OutputStream out1 = new FileOutputStream("PubKeyCollection.pkr");
            out1 = new ArmoredOutputStream(out1);

            thisCollection.encode(out1);
            out1.close();

            File inFile = new File("misc/Identities.txt");
            File tempF = new File("misc/tempFile.txt");

            BufferedReader readIn = new BufferedReader(new FileReader(inFile));
            BufferedWriter writeOut = new BufferedWriter(new FileWriter(tempF));

            String thisLine;

            while((thisLine = readIn.readLine()) != null) {
                String trimmedLine = thisLine.trim();
                System.out.println(trimmedLine);
                if(trimmedLine.equals(remove)) continue;
                writeOut.write(thisLine + System.getProperty("line.separator"));
            }
            writeOut.close();
            readIn.close();



            FileWriter out = new FileWriter("misc/Identities.txt",false);
            out.write(Utilities.readFile("misc/tempFile.txt",StandardCharsets.US_ASCII));
            out.close();
            tempF.delete();


        }else if(thisChar==('l')){
            System.out.println("List of public key IDs you currently store:");
            System.out.println(Utilities.readFile("Identities.txt", StandardCharsets.US_ASCII));
        }

        else{
            System.out.println("Please type a to add key or r to remove key");
        }
    }

}