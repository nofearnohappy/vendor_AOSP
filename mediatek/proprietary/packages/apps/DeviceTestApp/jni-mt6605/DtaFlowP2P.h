
namespace android {

typedef enum {
    DTA_P2P_NONE        = 0x00,
    DTA_P2P_INITIATOR   = 0x01,
    DTA_P2P_TARGET      = 0x02,

} DTA_P2P_TYPE;

int P2PTest(DTA_P2P_TYPE type);

}
