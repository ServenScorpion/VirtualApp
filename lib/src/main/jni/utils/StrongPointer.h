//
// Created by zhangsong on 18-3-6.
//

#ifndef VIRTUALAPP_STRONGPOINTER_H
#define VIRTUALAPP_STRONGPOINTER_H

namespace xdja {
namespace zs {

template <typename T> class sp
{
public:
    inline sp() : m_ptr(0) { }

    sp(T* other);
    sp(const sp<T>& other);
    template<typename U> sp(U* other);
    template<typename U> sp(const sp<U>& other);

    ~sp();

    inline  T&      operator* () const  { return *m_ptr; }
    inline  T*      operator-> () const { return m_ptr;  }
    inline  T*      get() const         { return m_ptr; }

private:
    template<typename Y> friend class sp;
    T* m_ptr;
};

template<typename T> sp<T>::sp(T* other) : m_ptr(other)
{
    if (other) other->incStrong(this);
}

template<typename T> sp<T>::sp(const sp<T>& other) : m_ptr(other.m_ptr)
{
    if (m_ptr) m_ptr->incStrong(this);
}

template<typename T> template<typename U> sp<T>::sp(U* other) : m_ptr(other)
{
        if (other) ((T*)other)->incStrong(this);
}

template<typename T> template<typename U> sp<T>::sp(const sp<U>& other) : m_ptr(other.m_ptr)
{
    if (m_ptr) m_ptr->incStrong(this);
}

template<typename T> sp<T>::~sp()
{
    if (m_ptr) m_ptr->decStrong(this);
}

}
}
#endif //VIRTUALAPP_STRONGPOINTER_H
