#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <errno.h>
#include <netdb.h>
#include <fcntl.h>
#include <dirent.h>
#include <time.h>


enum PROTOCOL_TYPE{
	PROTOCOL_ICMP=1,
	PROTOCOL_IPV4=4,
	PROTOCOL_TCP=6,
	PROTOCOL_UDP=17,
	PROTOCOL_IPV6=41,
	PROTOCOL_GRE=47,
	PROTOCOL_ESP=50,
	PROTOCOL_AH=51,
	PROTOCOL_ICMPV6=58,
	PROTOCOL_IPCOMP=108,
	PROTOCOL_L2TP=115	
};

/*flush all SA*/
extern int setkey_flushSAD(void);

/*flush all SP*/
extern int setkey_flushSPD(void);

/*delete one SA entry*/
extern int setkey_deleteSA(char * src,char * dst,char * ipsec_type,char * spi_src);

/*delete one SP entry*/
int setkey_deleteSP(char * src,char * dst,enum PROTOCOL_TYPE protocol,char * src_port,char * dst_port,char * direction);

/*dump all SA */
extern int dump_setkeySA(void);

/*dump all SP */
extern int dump_setkeySP(void);

/*set one SA*/
/*ipsec_type:ah esp
  mode:transport tunnel
  encrp_algo_src:encryption algorithm,des-cbc,3des-cbc...
  encrp_algo_src:key of encryption algorithm
  intergrity_algo_src:authentication algorithm ,hmac-md5,hmac-sha1       
  intergrity_key_src:key of authentication algorithm
*/
extern int setkey_setSA(char * ip_src,char * ip_dst,char * ipsec_type,char * spi_src,char * mode, char * encrp_algo_src,char * encrp_key_src,char * intergrity_algo_src,char * intergrity_key_src,int u_id);
/*set one SP of one direction just for transport mode*/
/*protocol:tcp icmp udp icmp6 ip4 gre
  direction:src->dst */
extern int setkey_SP(char * src_range,char * dst_range,enum PROTOCOL_TYPE protocol,char * port_src,char * port_dst,char * ipsec_type,char * mode, char * direction,int u_id);

/*set one SP of one direction, just for tunnel mode*/
/*protocol:tcp icmp udp icmp6 ip4 gre
  direction:src->dst
src_tunnel,dst_tunnel: tunnel src ip tunnel dst ip */
extern int setkey_SP_tunnel(char * src_range,char * dst_range,enum PROTOCOL_TYPE protocol,char * port_src,char * port_dst,char * src_tunnel,char * dst_tunnel,char * ipsec_type,char * mode, char * direction,int u_id);

/*set one SP of one direction, for 2 layers' ipsec--tunnel mode+transport mode or transport mode+tunnel mode*/
/*protocol:tcp icmp udp icmp6 ip4 gre
  direction:src->dst
src_tunnel,dst_tunnel: tunnel src ip tunnel dst ip */
extern int setkey_SP_tunnel_transport(char * src_range,char * dst_range,enum PROTOCOL_TYPE protocol,char * port_src,char * port_dst,char * src_tunnel,char * dst_tunnel,char * ipsec_type1,char * mode1, char * ipsec_type2,char * mode2,char * direction,int u_id1,int u_id2);

/*flush SA\SP from setkey.conf*/
extern int flush_SA_SP_exist();

