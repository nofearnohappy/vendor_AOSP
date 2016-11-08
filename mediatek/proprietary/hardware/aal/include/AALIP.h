#ifndef __AAL_IP_H__
#define __AAL_IP_H__

#if defined(MT6582) || defined(MT6592)
#define __AAL_10__
#elif defined(MT6797) || defined(MT6755) || defined(MT6595) || defined(MT6795) || defined(MT6752) || \
        defined(MT6735) || defined(MT6735M) || defined(MT6753) || defined(MT6580)
#define __AAL_20__
#endif

#endif
