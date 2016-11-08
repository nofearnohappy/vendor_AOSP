/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
#ifndef MT_HEADER_H_
#define MT_HEADER_H_

#include <stdint.h>

#define HEADER_SIZE 512
#define COMPARE_NUM 32

#define RESERVE_CONFIG_UBUFFER(buffer,len) unsigned char *buffer = (unsigned char *)mtk_malloc(len)
#define RELEASE_CONFIG_BUFFER(buffer)      free(buffer)

#define rotl32(w, s) (((w) << (s)) | ((w) >> (0x20 - (s))))
#define FI(b, c, d) (c ^ (b | ~d))
#define FH(b, c, d) (b ^ c ^ d)
#define FF(b, c, d) (d ^ (b & (c ^ d)))
#define FG(b, c, d) FF(d, b, c)


typedef struct md5_ctx_t {
    uint32_t A;
    uint32_t B;
    uint32_t C;
    uint32_t D;
    uint64_t total;
    uint32_t buflen;
    char buffer[128];
} md5_ctx_t;

typedef struct my_header_t {
    uint64_t size;
    unsigned int file_count;
    char checksum[32];
    int encrypt;
    int raw_file_count;
} my_header_t;

uint8_t *hash_file(const char *filename);
void my_md5_end(void *my_resbuf, md5_ctx_t *my_ctx);
void my_md5_begin(md5_ctx_t *ctx);
void my_md5_hash(const void *my_buffer, size_t len, md5_ctx_t *my_ctx);
unsigned char *hash_bin_to_hex(unsigned char *hash_value, unsigned hash_length);
int set_encrypted_custom_header(unsigned char* out_buf, int out_buf_size, my_header_t* my_header);
int get_encrypted_custom_header(unsigned char* input_buf, int input_buf_size, my_header_t* my_header);

#endif
