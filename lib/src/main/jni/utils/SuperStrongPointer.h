//
// Created by zhangsong on 18-3-8.
//

#ifndef VIRTUALAPP_SUPERSTRONGPOINTER_H
#define VIRTUALAPP_SUPERSTRONGPOINTER_H

#include "RefBase.h"

#include <map>

namespace xdja
{
    namespace zs
    {

        template <class T> class ssp : public LightRefBase<ssp<T>>
        {
        public:
            inline ssp() : m_ptr(0) { }

            ssp(T* other);

            ~ssp();

            inline  T&      operator* () const  { return *m_ptr; }
            inline  T*      operator-> () const { return m_ptr;  }
            inline  T*      get() const         { return m_ptr; }

        private:
            template<typename Y> friend class ssp;
            T* m_ptr;
        };

        template<typename T> ssp<T>::ssp(T* other) : m_ptr(other)
        {
            if (other) other->incStrong(this);
        }

        template<typename T> ssp<T>::~ssp()
        {
            if (m_ptr) m_ptr->decStrong(this);
        }
    }
}
#endif //VIRTUALAPP_SUPERSTRONGPOINTER_H
