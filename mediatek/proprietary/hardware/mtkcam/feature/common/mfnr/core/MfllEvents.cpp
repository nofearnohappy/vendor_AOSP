#include "MfllEvents.h"
#include "MfllLog.h"

using android::sp;
using namespace mfll;

IMfllEvents* IMfllEvents::createInstance(void)
{
    return (IMfllEvents*)new MfllEvents;
}

MfllEvents::MfllEvents(void)
{
    size_t size = (size_t)EventType_Size;
    for (size_t i = 0; i < size; i++) {
        m_vvRegisteredListeners.push_back( std::vector< sp<IMfllEventListener> >() );
    }
}

MfllEvents::~MfllEvents(void)
{
}

void MfllEvents::registerEventListener(const enum EventType &t, const sp<IMfllEventListener> &event)
{
    if (t == EventType_Size) {
        for (size_t i = 0; i < (size_t)EventType_Size; i++) {
            m_vvRegisteredListeners[i].push_back(event);
        }
    }
    else {
        m_vvRegisteredListeners[(size_t)t].push_back(event);
    }
}

void MfllEvents::removeEventListener(const enum EventType &t, const IMfllEventListener *event)
{
    EventItr itr;

    if (t == EventType_Size) {
        for (size_t i = 0; i < (size_t)EventType_Size; i++) {
            for (itr = m_vvRegisteredListeners[i].begin(); itr != m_vvRegisteredListeners[i].end(); itr++) {
                if ((*itr).get() == event) {
                    m_vvRegisteredListeners[i].erase(itr);
                }
            }
        }
    }
    else {
        size_t i = (size_t)t;
        for (itr = m_vvRegisteredListeners[i].begin(); itr != m_vvRegisteredListeners[i].end(); itr++) {
            if ((*itr).get() == event) {
                m_vvRegisteredListeners[i].erase(itr);
            }
        }
    }
}

void MfllEvents::onEvent(EventType t, MfllEventStatus_t &status, void *mfllCore, void *param1, void *param2)
{
    std::vector< sp<IMfllEventListener> > *v = &m_vvRegisteredListeners[(size_t)t];

    for(EventItr itr = v->begin(); itr != v->end(); itr++) {
        (*itr)->onEvent(t, status, mfllCore, param1, param2);
    }
}

void MfllEvents::doneEvent(EventType t, MfllEventStatus_t &status, void *mfllCore, void *param1, void *param2)
{
    std::vector< sp<IMfllEventListener> > *v = &m_vvRegisteredListeners[(size_t)t];

    for(EventItr itr = v->begin(); itr != v->end(); itr++) {
        (*itr)->doneEvent(t, status, mfllCore, param1, param2);
    }
}
