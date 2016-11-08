#include <binder/Parcel.h>

#include <ISensorHubClient.h>
#include <ISensorHubServer.h>

namespace android {

enum {
    GET_CONTEXT_LIST = IBinder::FIRST_CALL_TRANSACTION,
    GET_ACTION_LIST,
    ATTACH_CLIENT,
    DETACH_CLIENT,
    REQUEST_ACTION,
    UPDATE_ACTION,
    UPDATE_CONDITION,
    CANCEL_ACTION,
    ENABLE_GESTURE_WAKEUP,
};

class BpSensorHubServer: public BpInterface<ISensorHubServer>
{
public:
    BpSensorHubServer(const sp<IBinder>& impl)
        : BpInterface<ISensorHubServer>(impl)
    {
    }

    virtual Vector<int> getContextList()
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        remote()->transact(GET_CONTEXT_LIST, data, &reply);
        size_t size = reply.readInt32();
        Vector<int> v;
        for (size_t i = 0; i < size; i++) {
            v.add(reply.readInt32());
        }
        return v;
    }
	
    virtual Vector<int> getActionList()
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        remote()->transact(GET_ACTION_LIST, data, &reply);
        size_t size = reply.readInt32();
        Vector<int> v;
        for (size_t i = 0; i < size; i++) {
            v.add(reply.readInt32());
        }
        return v;
    }
    
    virtual void attachClient(const sp<ISensorHubClient>& client)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        data.writeStrongBinder(IInterface::asBinder(client));
        remote()->transact(ATTACH_CLIENT, data, &reply);
    }

    virtual void detachClient(const sp<ISensorHubClient>& client)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        data.writeStrongBinder(IInterface::asBinder(client));
        remote()->transact(DETACH_CLIENT, data, &reply);
    }

    virtual int requestAction(const SensorCondition& condition, const SensorAction& action)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        data.write(condition);
        data.write(action);
        remote()->transact(REQUEST_ACTION, data, &reply);
        return reply.readInt32();
    }

    virtual bool updateAction(int requestId, const SensorCondition& condition, const SensorAction& action)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        data.writeInt32(requestId);
        data.write(condition);
        data.write(action);
        remote()->transact(UPDATE_ACTION, data, &reply);
        return reply.readInt32();
    }

    virtual bool updateCondition(int requestId, const SensorCondition& condition)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        data.writeInt32(requestId);
        data.write(condition);
        remote()->transact(UPDATE_CONDITION, data, &reply);
        return reply.readInt32();
    }
	
    virtual bool cancelAction(int requestId)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        data.writeInt32(requestId);
        remote()->transact(CANCEL_ACTION, data, &reply);
        return reply.readInt32();
    }

    virtual bool enableGestureWakeup(bool enable) 
    {
        Parcel data, reply;
        data.writeInterfaceToken(ISensorHubServer::getInterfaceDescriptor());
        data.writeInt32(enable);
        remote()->transact(ENABLE_GESTURE_WAKEUP, data, &reply);
        return reply.readInt32();
    }
};

IMPLEMENT_META_INTERFACE(SensorHubServer, "com.mediatek.sensorhub.ISensorHubServer");

// ----------------------------------------------------------------------

status_t BnSensorHubServer::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch (code) {
        case GET_CONTEXT_LIST : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            Vector<int> v = getContextList();
            size_t size = v.size();
            reply->writeInt32(size);
            for (size_t i = 0; i < size; i++) {
                reply->writeInt32(v[i]);
            }
            return NO_ERROR;
        } break;
        case GET_ACTION_LIST : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            Vector<int> v = getActionList();
            size_t size = v.size();
            reply->writeInt32(size);
            for (size_t i = 0; i < size; i++) {
                reply->writeInt32(v[i]);
            }
            return NO_ERROR;
        } break;
        case ATTACH_CLIENT : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            sp<ISensorHubClient> client = interface_cast<ISensorHubClient>(data.readStrongBinder());
            attachClient(client);
            return NO_ERROR;
        } break;
        case DETACH_CLIENT : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            sp<ISensorHubClient> client = interface_cast<ISensorHubClient>(data.readStrongBinder());
            detachClient(client);
            return NO_ERROR;
        } break;
        case REQUEST_ACTION : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            SensorCondition condition;
            data.read(condition);
            SensorAction action;
            data.read(action);
            int request = requestAction(condition, action);
            reply->writeInt32(request);
            return NO_ERROR;
        } break;
        case UPDATE_ACTION : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            int requestId = data.readInt32();
            SensorCondition condition;
            data.read(condition);
            SensorAction action;
            data.read(action);
            bool request = updateAction(requestId, condition, action);
            reply->writeInt32(request);
            return NO_ERROR;
        } break;
        case UPDATE_CONDITION : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            int requestId = data.readInt32();
            SensorCondition condition;
            data.read(condition);
            bool request = updateCondition(requestId, condition);
            reply->writeInt32(request);
            return NO_ERROR;
        } break;
        case CANCEL_ACTION : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            int requestId = data.readInt32();
            bool request = cancelAction(requestId);
            reply->writeInt32(request);
            return NO_ERROR;
        } break;
		case ENABLE_GESTURE_WAKEUP : {
            CHECK_INTERFACE(ISensorHubServer, data, reply);
            bool enable = data.readInt32();
            bool request = enableGestureWakeup(enable);
            reply->writeInt32(request);
            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

}; // namespace android
