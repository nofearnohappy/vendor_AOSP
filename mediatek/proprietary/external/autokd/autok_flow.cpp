#include <unistd.h>
#include <sys/stat.h>
#include "autok.h"
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <string>
#include <iostream>
#include <list>
#include <set>
#include <cutils/properties.h>
#define BUF_LEN     1024
#define PROP_VALUE_MAX  92
//#include "property_service.h"
#include "errno.h"
//#include <linux/syswait.h>

extern std::list<struct host_progress*> get_ready();
extern std::list<unsigned int> get_nvram_voltages(int id);
extern int set_suggest_vol(std::list<unsigned int> vol_list);
std::list<unsigned int> get_data_voltages(int id);
int is_file_valid(char *fname)
{
    struct stat fstat;
    if (lstat(fname,&fstat)==-1)
        return -ENFILE;
    if (!S_ISREG(fstat.st_mode))
        return -ENOENT;
    if(!fstat.st_size)
        return -ENOSPC;
    return fstat.st_size;
}


int is_nvram_mode()
{
#if 1    
    int get_time = 5;    
    char tmp[PROP_VALUE_MAX];
    char bootmode[32] = "";
    while(get_time--!=0 && property_get("ro.bootmode", tmp, NULL)){
        usleep(100000);
    }
    if(property_get("init.svc.meta_tst", tmp, tmp) > 0){
        LOGD("Current Boot Mode:%s\n", "Meta Mode");
        return 1;  
    }
    strlcpy(bootmode, tmp, sizeof(bootmode));
    LOGD("Current Boot Mode:%s\n", bootmode);
    if (!strcmp(bootmode,"factory") || (NULL!=strstr(bootmode, "meta"))){
        return 1;
    }
    return 0;   
#else
    int get_time = 5;
    char bootmode[32];
    char tmp[PROP_VALUE_MAX];
    while(get_time--!=0 && property_get("autok_bootmode", tmp, NULL)){
        usleep(100000);
    }
    strlcpy(bootmode, tmp, sizeof(bootmode));
    if (!strcmp(bootmode,"factory")){
        return 1;
    }
    return 0;
#endif    
}

struct res_data{
    int id;
    unsigned int voltage;
    char filepath[256];  
};

int do_host_autok(int id)
{
    int i, j;
    int param_count, nvram_param_count;
    int vol_count;
    int vol_index;
    struct autok_predata predata;
    struct autok_predata full_data;
    unsigned int *param_list;
    unsigned int *vol_list;
    unsigned int voltage;
    std::list<struct res_data*> res_list;
    std::list<struct res_data*>::iterator it_res;
    std::list<unsigned int> nvram_vol_list;
    std::list<unsigned int> data_vol_list;
    std::set<unsigned int> vol_index_set;
    std::list<unsigned int> abort_list;
    std::list<unsigned int>::iterator it_vol;
    struct res_data *p_res = NULL;
    struct res_data *p_temp_res = NULL;
    struct timespec tstart={0,0}, tend={0,0};
    char *data_buf = NULL;
    int data_length = 0;
    char devnode[BUF_LEN]=""; 
    char resnode[BUF_LEN]="";
    char data_node[BUF_LEN]="";
    int need_to_recover = 0;
    int need_tuning = 0;
    int ret = 0;
    int stg1done = 0;  
    
    init_autok_nvram();    
    param_count = get_param_count();    
    nvram_param_count = get_nvram_param_count(id);
    if(nvram_param_count!=0 && nvram_param_count!=param_count){
        system("rm -rf /data/autok_*");
        system("rm -rf /data/nvram/APCFG/APRDCL/SDIO");
        close_nvram();
        init_autok_nvram();
        need_to_recover = 1;
    }
    // [rome] If nvram voltages > suggest vol->add nvram vols into suggest_vol
    nvram_vol_list = get_nvram_voltages(id);
    data_vol_list = get_data_voltages(id);
    set_suggest_vol(nvram_vol_list);
    set_suggest_vol(data_vol_list);
    if((vol_count = get_suggest_vols(&vol_list))>0){
        AUTOK_FREE(g_autok_vcore);
        g_autok_vcore = vol_list;
        g_vcore_no = vol_count;
    }
    nvram_vol_list.clear();
    data_vol_list.clear();
    LOGD("Current vcore_number:%d\n", g_vcore_no);
    
    //duplicate nvram data partition
    for(i=0; i<g_vcore_no; i++){
        snprintf(data_node, BUF_LEN, "%s/%s_%d_%d", AUTOK_RES_PATH, RESULT_FILE_PREFIX, id, g_autok_vcore[i]);
        // [Check environment]
        // 1. nvram data partition should exist
        // 2. autok parameter in data partion should not exist
        // 3. param_count should be the same 
        if(is_nvram_data_exist(id, g_autok_vcore[i])>0 && is_file_valid(data_node)<=0){
            //data_copy(nvram_node, data_node);
            read_from_nvram(id, g_autok_vcore[i], (unsigned char**)&data_buf, &data_length);
            if(write_to_file(data_node, data_buf, data_length)<0){
                AUTOK_FREE(data_buf);
                return -1;
            }
            AUTOK_FREE(data_buf);
            LOGD("duplicata from [%s] to [%s]\n", AUTOK_NVRAM_PATH, data_node);
        }
    }    
    
    //check autok folder
    for(i=0; i<g_vcore_no; i++){
        LOGI("1-0 Pre-check autok environment\n");
        // /data/autok_[id]_[vol]
        snprintf(resnode, BUF_LEN, "%s/%s_%d_%d", AUTOK_RES_PATH, RESULT_FILE_PREFIX, id, g_autok_vcore[i]);
        p_res = (struct res_data*)malloc(sizeof(struct res_data));
        p_res->id = id;
        p_res->voltage = g_autok_vcore[i];
        memset(p_res->filepath, 0, sizeof(p_res->filepath)/sizeof(p_res->filepath[0]));
        strncpy(p_res->filepath, resnode, (sizeof(p_res->filepath)-1));
        
        // Pre-test for autok format
        predata = get_param(p_res->filepath);
        if(predata.vol_count == 0){
            LOGI("Predata format error\n");
            snprintf(data_node, BUF_LEN, "rm -rf %s/%s_%d_%d", AUTOK_RES_PATH, RESULT_FILE_PREFIX, id, g_autok_vcore[i]);
            system(data_node);
        }            
        release_predata(&predata);
        res_list.push_back(p_res);            
        
        LOGD("1-1 Check /data/autok_[id]_[vol] existence\n");
        // If /data/autok_id_vol is not exist, it means no tuning
        if(is_file_valid(resnode)<=0){
            need_tuning = 1;
            set_stage1_log(id, 1);
            //undo_vol_list.push_back(g_autok_vcore[i]);
            set_stage1_voltage(id, g_autok_vcore[i]);
            set_stage1_done(id, 0);
            LOGD("[%s] set done to 0\n", resnode);
            // prepare zero data to drive autok algorithm 
            param_list = (unsigned int*)malloc(sizeof(unsigned int)*param_count);
            for(j=0; j<param_count; j++){
                param_list[j] = 0;        
            }
            vol_count = 1;            
            vol_list = (unsigned int*)malloc(sizeof(unsigned int)*vol_count); 
            vol_list[0] = g_autok_vcore[i];                  
            pack_param(&predata, vol_list, vol_count, param_list, param_count);
            clock_gettime(CLOCK_MONOTONIC, &tstart);
            set_stage1_params(id, &predata);
            
            LOGD("operation col_count[%d] vcore[%d] param_count[%d]\n", vol_count, vol_list[0], param_count);     
            release_predata(&predata);
            LOGD("release col_count[%d] param_count[%d]\n", vol_count, param_count); 
            // Wait for autok stage1 for a specific voltage done
            // [FIXME] can switch to uevent?
            while(1){
                stg1done = get_stage1_done(id);
                if(stg1done)
                    break;
                usleep(10*1000); 
            }
            
            clock_gettime(CLOCK_MONOTONIC, &tend);
            LOGI("autok once %.5f seconds\n",((double)tend.tv_sec + 1.0e-9*tend.tv_nsec) - ((double)tstart.tv_sec + 1.0e-9*tstart.tv_nsec));
            set_debug(0);
            snprintf(devnode, BUF_LEN, "%s/%d/%s", STAGE1_DEVNODE, id, "PARAMS");
            LOGD("Set_Debug(0), stg1done = %d\n", stg1done);
            if(stg1done != 3){  // if it's not abort by DVFS
                if(!is_nvram_mode()){
                    LOGI("[NON_NV]From dev[%s] to res[%s]\n", devnode, resnode);
                    from_dev_to_data(devnode, resnode);
                    // For recover different version param_count
                    if(need_to_recover){
                        write_dev_to_nvram(devnode, id);
                    }
                } else {
                    LOGI("[NV]From dev[%s] to nvram\n", devnode);
                    from_dev_to_data(devnode, resnode);
                    write_dev_to_nvram(devnode, id);
                }
            } else{ // abort by DVFS
                LOGI(">abort by DVFS< stg1done: %d vcore: %d\n",stg1done,g_autok_vcore[i]);        	
                abort_list.push_back(g_autok_vcore[i]);                
            }
        }
        LOGI("1-2 Do autok-I for vol[%u] done\n", g_autok_vcore[i]);
    }
    
    LOGI("1-3 Remove abort voltage\n");
    std::list<struct res_data*> bak_res_list(res_list);
    for(it_res = bak_res_list.begin(); it_res != bak_res_list.end(); ++it_res){
        p_temp_res = *it_res;
        for(it_vol=abort_list.begin(); it_vol!=abort_list.end(); ++it_vol){
            if(p_temp_res->voltage == *it_vol){
                g_vcore_no--;
                LOGI("remove voltage: %d\n",p_temp_res->voltage);        	
                res_list.remove(p_temp_res);
                AUTOK_FREE(p_temp_res);
                LOGI("current res_list_size:%d\n", res_list.size());
                break;
            }
        }
    }
    bak_res_list.clear();
    
    LOGI("2-1 Collect Stage1 Data\n");
    full_data.vol_count = g_vcore_no;
    full_data.param_count = param_count;
    full_data.vol_list = (unsigned int*)malloc(sizeof(unsigned int)*g_vcore_no);
    full_data.ai_data = (U_AUTOK_INTERFACE_DATA**)malloc(sizeof(U_AUTOK_INTERFACE_DATA*)*g_vcore_no);        
    
    LOGI("2-2 Switch voltage index with assending order\n");
    for(it_res=res_list.begin(); it_res!=res_list.end(); ++it_res){
        p_temp_res = *it_res;
        vol_index_set.insert(p_temp_res->voltage);
        LOGD("Insert voltage[%d] to vol_index_set\n", p_temp_res->voltage);
    }

    LOGI("2-3 Merge stage1 data into stage2\n");
    for(it_res=res_list.begin(); it_res!=res_list.end(); ++it_res){            
        p_temp_res = *it_res;
        vol_index = std::distance(vol_index_set.begin(), vol_index_set.find(p_temp_res->voltage));
        predata = get_param((char*)p_temp_res->filepath);
        full_data.vol_list[vol_index] = p_temp_res->voltage;
        LOGI("Add voltage:%u to index:%d\n", p_temp_res->voltage, vol_index);
        full_data.ai_data[vol_index] = (U_AUTOK_INTERFACE_DATA*)malloc(sizeof(U_AUTOK_INTERFACE_DATA)*param_count);
        if(predata.vol_count > 0){
            memcpy(full_data.ai_data[vol_index], predata.ai_data[0], sizeof(U_AUTOK_INTERFACE_DATA)*param_count);
        }    
        release_predata(&predata);
        AUTOK_FREE(p_temp_res);
    }
    res_list.clear();
    LOGI("2-4 Store full data into autok stage2 node\n");
    set_stage2(id, &full_data);
    release_predata(&full_data);
    
    LOGI("3-1 Restore autok execution log\n");
    if(need_tuning){        
        snprintf(devnode, BUF_LEN, "%s/%s_%d_log", AUTOK_RES_PATH, RESULT_FILE_PREFIX, id);
        write_full_log(devnode);         
        need_tuning = 0;
    }
    
    LOGI("3-2 Check if calibration is fail or not\n");
    if (stg1done == 2) {
        system("rm -rf /data/autok_*");
        system("rm -rf /data/nvram/APCFG/APRDCL/SDIO");
        ret = -2;
    }
    
    LOGI("3-3 Terminate autok procedure\n");
    set_stage1_log(id, 0);
    close_nvram();
    free(vol_list);
    free(param_list);
    return ret;
}

int wait_autok_done(int id)
{
    int is_stage2_done = 0;
    std::list<struct host_progress*>::iterator it_prog;
    std::list<struct host_progress*> host_prog;
    while(!is_stage2_done){
        host_prog = get_ready();
        for (it_prog=host_prog.begin(); it_prog!=host_prog.end() ; ++it_prog){
            struct host_progress *prog = *it_prog;
            if(prog->host_id==id){
                if(prog->is_done == 1){
                    is_stage2_done = 1;
                }
                break;
            }
            AUTOK_FREE(prog);
        }
        usleep(30*1000);
    }
    return 0;
}

static int is_nothing_to_do(std::list<struct host_progress*> host_prog)
{
    std::list<struct host_progress*>::iterator it_prog;
    if(host_prog.size() <= 0){
        return 1;
    }
    for (it_prog=host_prog.begin(); it_prog!=host_prog.end() ; ++it_prog){
        struct host_progress *prog = *it_prog;  
        if(prog->host_id>=0 && !prog->is_done){
            return 0;
        }
    }
    return 1;
}

int autok_flow()
{
    std::list<struct host_progress*>::iterator it_prog;
    std::list<struct host_progress*> host_prog;
    struct host_progress *prog;
    unsigned int *vol_list;
    int vol_count = 0;
    struct timespec tstart={0,0}, tend={0,0};
    int uevent_hid = 0;
    int ret = 0;
    char time_stamp[100]={0};
    bool use_uevent = false;
    
    while(1){
        // [FIXME] uevent to trigger host autok operation?        
        if(use_uevent/* && is_nothing_to_do(host_prog)*/){
            uevent_hid = 0;
            if(wait_sdio_uevent(&uevent_hid, "s2_ready") != 0)
                return -1;
        }
        host_prog = get_ready();
        for (it_prog=host_prog.begin(); it_prog!=host_prog.end() ; ++it_prog){
            prog = *it_prog;
            if (prog->is_done == 0xFFFFFFFF) {
                system("rm -rf /data/autok_*");
                system("rm -rf /data/nvram/APCFG/APRDCL/SDIO");
                prog->is_done = 0;                          
            }
            if(prog->host_id>=0 && !prog->is_done){
                // For the special case which provide vol_list from kernel side        
                LOGI("0.1 Autok runs at host %d\n", prog->host_id);
                clock_gettime(CLOCK_MONOTONIC, &tstart);
                ret = do_host_autok(prog->host_id);
                if(ret == -1)
                    return -1;
                //uevent_hid = 0;
                LOGD("0.2 Start to wait for autok done\n");
                if(/*uevent_hid!=prog->host_id &&*/ wait_autok_done(prog->host_id)==-1)
                    return -1;
                notify_autok_done();
                clock_gettime(CLOCK_MONOTONIC, &tend);                
                LOGI("[Performance] Autok computation took about %.5f seconds\n",((double)tend.tv_sec + 1.0e-9*tend.tv_nsec) - ((double)tstart.tv_sec + 1.0e-9*tstart.tv_nsec));
                snprintf(time_stamp, 100, "%.5f", ((double)tend.tv_sec + 1.0e-9*tend.tv_nsec) - ((double)tstart.tv_sec + 1.0e-9*tstart.tv_nsec));
                property_set("sys.autok.is_done", time_stamp);
                // First time: Polling, Other time: uevent 
                if (ret == 0)
                    use_uevent = true;
            }
            AUTOK_FREE(prog);
        }
        if (!use_uevent)
            usleep(500*1000);
    }
    return 0;
}
