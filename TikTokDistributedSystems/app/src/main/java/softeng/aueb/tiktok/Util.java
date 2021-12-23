package softeng.aueb.tiktok;

import java.io.*;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Util {
    //    public static final String BIG_PRIME_NUMBER = "15485867";
    public static final String BIG_PRIME_NUMBER = "7919";
    public static final boolean DEBUG = true;

    public static byte[] loadVideoFromDiskToRam(VideoFile videoFile){
        try{
            File file = new File(""+videoFile.path);
            FileInputStream fis = new FileInputStream(file);
            byte[] fileData = new byte[(int)file.length()];
            fis.read(fileData);
            return fileData;
        }catch (FileNotFoundException e){
            System.err.println("File not found exception");
        }catch (IOException e){
            System.err.println("IOException from loadSongFromDiskToRam() in util package");
        }
        return null;
    }


    public static List<byte[]> chunkifyFile(byte[] data){
        int chunkSize = 512;
        return chunkifyFile(data, chunkSize);
    }

    public static List<byte[]> chunkifyFile(byte[] data, int chunkSize){
        List<byte[]> result = new ArrayList<>();
        int chunks = data.length / chunkSize;

        for (int i = 0; i < chunks ; i++) {
            byte[] current = new byte[chunkSize];
            System.arraycopy(data, i * chunkSize, current, 0, chunkSize);
            result.add(current);
        }

        //Add last chunk
        boolean isRemaining = data.length % chunkSize != 0;
        if (isRemaining){
            int remaining = data.length % chunkSize;
            int offset = chunks * chunkSize;

            byte[] current = new byte[remaining];
            System.arraycopy(data, offset, current, 0, remaining);

            result.add(current);
        }

        return result;
    }



    public static int getModMd5(String value){
        BigInteger bi = getMd5(value);
        BigInteger m = new BigInteger(BIG_PRIME_NUMBER);
        BigInteger moded = bi.mod(m);

        return Integer.parseInt(moded.toString());
    }


    private static BigInteger getMd5(String input)
    {
        try {

            // Static getInstance method is called with hashing MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            // digest() method is called to calculate message digest
            //  of an input digest() return array of byte
            byte[] messageDigest = md.digest(input.getBytes());
            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);
            return no;
        }
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }



    public static class Pair<T1, T2>implements Serializable {

        public T1 item1;
        public T2 item2;

        private static final long serialVersionUID = -2723363051271966964L;

        public Pair(T1 item1, T2 item2) {
            this.item1 = item1;
            this.item2 = item2;
        }
    }
}
