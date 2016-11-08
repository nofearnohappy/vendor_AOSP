#!/system/bin/sh

# change KB/DKB device node
/system/bin/sh -c '/system/bin/chown root:system `/system/bin/readlink -f /dev/block/platform/mtk-msdc.0/by-name/KB`'
/system/bin/sh -c '/system/bin/chmod 0660 `/system/bin/readlink -f /dev/block/platform/mtk-msdc.0/by-name/KB`'
/system/bin/sh -c '/system/bin/chown root:system `/system/bin/readlink -f /dev/block/platform/mtk-msdc.0/by-name/DKB`'
/system/bin/sh -c '/system/bin/chmod 0660 `/system/bin/readlink -f /dev/block/platform/mtk-msdc.0/by-name/DKB`'