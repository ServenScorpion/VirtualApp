#include <stdio.h>
#include <pthread.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

#include "RefBase.h"
#include "StrongPointer.h"
#include "SuperStrongPointer.h"
#include "atomicVessel.h"
#include "releaser.h"

//自动释放类
class refTest : public xdja::zs::LightRefBase<refTest>
{
  public:
    void p() { printf("%p ref [%d]\n", (void *)this, getStrongCount()); }
};

int myRandom()
{
  timespec time;
  clock_gettime(CLOCK_REALTIME, &time);  //获取相对于1970到现在的秒数
  struct tm nowTime;
  localtime_r(&time.tv_sec, &nowTime);

  srand(nowTime.tm_sec);

  return rand();
}

atomicVessel tmp;
releaser<refTest> rl;

bool isHit()
{
  int hitpoints[] = {17, 64, 28, 87, 99, 33, 44, 55, 77, 11, 22, 55, 66, 88};
  int rand = myRandom() % 100;
  for(int i = 0; i < sizeof(hitpoints)/sizeof(hitpoints[0]); i++)
  {
    if(hitpoints[i] == rand)
      return true;
  }

  return false;
}

void * threadfun(void * param)
{
  do 
  {
    {
    xdja::zs::sp<refTest> sp((refTest *)tmp.get());

    if(sp.get() == nullptr)
    {
      printf("atomicVessel.get() return null, so thread %ld return ! \n", (long int)syscall(224));
      return 0;
    }

    //关闭几率 5%
    if(isHit())
    {
      printf("release in thread %ld!\n", (long int)syscall(224));

      //清空 防止后续得到
      tmp.reset();
      //释放
      rl.release(sp.get());
    }
    else
    {
      //显示引用计数
      printf("ssp ref %d in thread %ld return ! \r", sp->getStrongCount(), (long int)syscall(224));
    }
    }

    usleep(myRandom() % 1000 + 800); 
  }while(true);
}

#define THREAD_NUM 100
#define TEST_TIMES 1001000

int main(int argc, char * argv[])
{
  long test_times = TEST_TIMES;
  do
  {
    refTest * p = new refTest();
    p->incStrong(0);
    
    printf("p = %p\n", p);

    tmp.set((uint32_t)p);

    //跑线程
    {
      pthread_t id[THREAD_NUM] = {0};

      for(int i = 0; i < THREAD_NUM; i++)
      {
        pthread_create(&id[i], 0, threadfun, 0);
        usleep(myRandom() % 20); 
      }

      for(int i = 0; i < THREAD_NUM; i++)
        pthread_join(id[i], 0);
    }

    printf("test process %0.2lf%%\n", ((1 - ((double)test_times) / TEST_TIMES) * 100));
  }while(--test_times > 0);

  rl.finish();
  printf("main exit !\n");

  return 0;
}
