#ifdef __cplusplus
extern "C" {
#endif

    void generateotp(char *key, int key_length, char *digest);
    int generateOtpDigits(char *key, int key_length);
#ifdef __cplusplus
}
#endif
