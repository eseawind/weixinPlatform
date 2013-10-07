package com.weixin.utility;

/* base */
import java.io.Serializable;
import java.util.List;
/* hibernate */
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;
//import org.hibernate.service.ServiceRegistry;
//import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
/* log */
//import org.apache.log4j.Logger;

/**
 * DAO 层的 Hibernate 实现
 * <br />
 * 该工具类封装了线程安全的数据库连接及读写操作. 同一个连接进行的读写都在
 * 同一个数据库连接内进行(以保持数据的一致性), 除非上层代码故意不那么做.
 * <br />
 * 这里提供一个读写例程
 * <br />
 <code>
    // 要进行读写操作需要先创建一个事务
    // 注意重复创建或关闭事务会导致不可预料的结果
    HbmDao.begin();

    // 然后就可以进行增删查改等业务
    User u = (User)HbmDao.get(User.class, 1);
    // 业务代码
    u.setName("foobar");
    // 写回
    HbmDao.saveOrUpdate(u);

    // 提交(commit)或回滚(rollback)事务
    HbmDao.commit();
    // 然后可以进行另外一个事务
    HbmDao.begin();
    u = HbmDao.get(User.class, 2);
    // ...或者关闭连接
    HbmDao.close();
 </code>
 * 就是这么简单...
 * <br />
 * 切记要提交并且关闭会话, 否则出现内存溢出, 读写失败什么的概不负责.
 */
public class HbmDao
{
  // SESSION MANAGEMENT
    private SessionFactory sf = null;

    private static ThreadLocal<Session> threadSession = new ThreadLocal<Session>()
    {
        protected Session initialValue()
        {
            return(HbmDao.Singleton.instance.newSession());
        }
    };

    // 据传这是出自 Google 工程师之手的 Singleton 写法...
    private static class Singleton
    {
        private static HbmDao instance = new HbmDao();
    }

    private HbmDao()
    {
        // 为了适配 hibernate 4 的写法...
        //Configuration cfg = new Configuration()
            //.configure();
        //ServiceRegistry sr = new ServiceRegistryBuilder()
            //.applySettings(
                //cfg.getProperties()
            //).buildServiceRegistry();
        //this.sf = cfg.buildSessionFactory(sr);

        // hibernate 3 及以下
        this.sf = new Configuration()
            .configure()
            .buildSessionFactory();

        return;
    }

    /**
     * 生成一个新的Session
     * <br />
     * 允许外部代码调用该方法自行生成新的会话, 通过该方法生成的会话不会被
     * 绑定到线程也不会被 HbmDao 管理, 调用者需要自行负责操作线程和会话.
     */
    public static Session newSession()
    {
        return(Singleton.instance.sf.openSession());
    }

    public static Session getSession()
    {
        return(threadSession.get());
    }

  // TRANSACTION
    /**
     * 封装数据库事务的 begin 操作
     * <br />
     * 使用线程唯一的数据库连接, 如果当前线程未创建连接或连接已关闭则自动创建一个新的.
     */
    public static void begin()
    {
        if (!threadSession.get().isOpen())
            threadSession.set(newSession());
        threadSession.get().beginTransaction();

        return;
    }

    /** 封装 flush 操作
     */
    public static void flush()
    {
        threadSession.get()
            .flush();
        return;
    }


    /**
     * 封装数据库事务的 commit 操作
     */
    public static void commit()
    {
        threadSession.get()
            .getTransaction()
            .commit();

        return;
    }

    /**
     * 封装数据库事务的 rollback 操作
     */
    public static void rollback()
    {
        threadSession.get()
            .getTransaction()
            .rollback();

        return;
    }

    /**
     * 封装数据库连接的 close 操作
     */
    public static void close()
    {
        threadSession.get()
            .close();

        return;
    }

  // DAO
    /** 按主键查询
     * @return Object 主键对应对象, 没有则返回 null
     */
    public static Object get(Class c, Serializable id)
    {
        return(
            threadSession.get()
                .get(c, id)
        );
    }

    /** 按照条件唯一选取
     * @return 符合条件的对象, 存在0个或多于一个结果都返回null
     */
    public static Object get(DetachedCriteria dc)
    {
        Session s = threadSession.get();
        Object o = null;

        try
        {
            o = dc.getExecutableCriteria(s)
                .uniqueResult();
        }
        catch (HibernateException ex)
        {
            ex.printStackTrace();
            //Logger.getLogger("librarica.dao")
                //.error(ex.toString());
        }

        return(o);

    }

    /** 创建或更新数据对象
     */
    public static void saveOrUpdate(Object o)
    {
        threadSession.get()
            .saveOrUpdate(o);

        return;
    }

    public static void save(Object o)
    {
        threadSession.get()
            .save(o);

        return;
    }

    public static void update(Object o)
    {
        threadSession.get()
            .update(o);
    }

    /** 删除数据对象
     */
    public static void delete(Object o)
    {
        threadSession.get()
            .delete(o);

        return;
    }

    /** 搜索
     * <br />
     * @param dc 条件
     * @param start 结果分页用, 从第#条记录开始
     * @param limit 结果分页用, 限制最多返回#条记录
     */
    public static List search(DetachedCriteria dc, Integer start, Integer limit)
    {
        Session s = threadSession.get();
        Criteria c = dc.getExecutableCriteria(s);

        if (start != null)
            c.setFirstResult(start);
        if (limit != null)
            c.setMaxResults(limit);

        List li = c.list();

        return(li);
    }

    /** 按照搜索条件计数
     */
    public static Long count(DetachedCriteria dc)
    {
        Session s = threadSession.get();

        Criteria c = dc.getExecutableCriteria(s)
            .setProjection(
                Projections.rowCount()
            );

        return((Long)c.uniqueResult());
    }
}
