public class Test {
    public static void main(String[] args) {
        String s = "�������� ���������!";

        try {
            AES aes = new AES();

            String enc_s = aes.encrypt(s);
            System.out.println(enc_s);

            String dec_s = aes.decrypt(enc_s);
            System.out.println(dec_s);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
