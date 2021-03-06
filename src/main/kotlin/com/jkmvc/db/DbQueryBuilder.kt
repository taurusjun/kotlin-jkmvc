package com.jkmvc.db

import com.jkmvc.common.findConstructor
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *  提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @author shijianhang
 * @date 2016-10-13
 */
open class DbQueryBuilder(db:Db = Db.getDb(), table:String = "" /*表名*/) :DbQueryBuilderDecoration(db, table)
{
    public constructor(dbName:String /* db名 */, table:String = "" /*表名*/):this(Db.getDb(dbName), table){
    }

    /**
     * 获得记录转换器
     * @param clazz 要转换的类
     * @return 转换的匿名函数
     */
    public override fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T) {
        // 1 如果是map类，则直接返回
        if(Map::class.java.isAssignableFrom(clazz.java)){
            return {
                it as T;
            }
        }
        // 2 否则，调用其构造函数
        // 获得类的构造函数
        val construtor = clazz.findConstructor(listOf(MutableMap::class.java))
        if(construtor == null)
            throw RuntimeException("类${clazz}没有构造函数constructor(MutableMap)");

        // 调用构造函数
        return {
            construtor.call(it) as T; // 转换一行数据: 直接调用构造函数
        }
    }

    /**
     * 编译sql
     *
     * @param action sql动作：select/insert/update/delete
     * @return Pair(sql, 参数)
     */
    public override fun compile(action:String):Pair<String, List<Any?>>
    {
        params.clear();

        // 动作子句 + 修饰子句
        val sql:StringBuilder = StringBuilder();
        this.action(action).compileAction(sql).compileDecoration(sql);
//        println(sql)
//        println(params)
        return Pair(sql.toString(), params);
    }

    /**
     * 查找多个： select 语句
     *
     * @param transform 转换函数
     * @return 列表
     */
    public override fun <T:Any> findAll(transform:(MutableMap<String, Any?>) -> T): List<T>{
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRows<T>(sql, params, transform)
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(transform:(MutableMap<String, Any?>) -> T): T?{
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRow<T>(sql, params, transform);
    }

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @return 影响行数|新增id
     */
    protected fun execute(action:String):Int
    {
        // 1 编译
        val (sql, params) = compile(action);

        // 2 执行 insert/update/delete
        return db.execute(sql, params);
    }

    /**
     * 统计行数： count语句
     * @return
     */
    public override fun count():Long
    {
        // 1 编译
        val (sql, params) = select(Pair("count(1)", "num")).compile("select");

        // 2 执行 select
        val (hasNext, count) = db.queryCell(sql, params);
        return if(hasNext)
                    count as Long;
                else
                    0
    }

    /**
     * 插入：insert语句
     * @return 新增的id
     */
    public override fun insert():Int
    {
        return execute("insert");
    }

    /**
     *	更新：update语句
     *	@return	bool
     */
    public override fun update():Boolean
    {
        return execute("update") > 0;
    }

    /**
     *	删除
     *	@return	bool
     */
    public override fun delete():Boolean
    {
        return execute("delete") > 0;
    }
}