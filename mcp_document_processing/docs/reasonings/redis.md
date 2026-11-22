# Redis integration

## The idea: Why using Redis 
- ConcurrentHashMap: Why replace it with Redis? Pros & Cons 

ConcurrentHashMap is in application datastructure. Which mean that when the application is terminated or reset, the saved data will be cleared also. 
What I is the saved data should be save some where else, that can be retrieved eventhough the application is reset or terminated. 

`Redis` is a inmemory database. Since it is in memory, the speed for query is fast. And the modification operations' results will be saved into it instead of stick with the lifecycle of the application.

### Why Redis
1. Nothing much, to be honest I chose Redis because I want to learn about working with Redis and about Redis itself.
2. About the data model, considering about what the application's mission: 
   - Receiving Documents from LLMs &rarr; Processes it and return the final result
   - LLMs can call a again with the same desired file &rarr; callID for idempotency key (**permanent persistence**)
   - Seeing the same callID with the same filepath &rarr; Return the result: The result should be long-term persisted since users (who're using llms) can prompt the llms to read the exactly files in another time. But I assumed that, they rearly do it, and if they do it, we just need to parse it again. To avoid storing large data, I choose to see the result as a **permanent data**

So yeah, first one is obviously why. And the second one is when I seriously look into what am I doing. So I want to use a in memory, a external cache system, for storing those data. And Redis is the most famous one.   

## About Redis: Transactions -- Atomic Operation in Redis

###  `WATCH/MULTI/EXEC/DISCARD`

#### `MULTI/EXEC`
- `MULTI` &approx; `BEGIN`; `EXEC` &approx; `COMMIT`
```sh
> MULTI
OK
> INCR foo
QUEUED
> INCR bar
QUEUED
> EXEC
1) (integer) 1
2) (integer) 1
```
- Every operation after `MULTI` will be queued and executed once EXEC is called.
- EXEC returns an array of replies.
- Redis does not support rollbacks of transactions since supporting rollbacks would have a significant impact on the simplicity and performance of Redis.

#### `DISCARD` 
- `DISCARD` &ne; `ROLLBACK`
- `DISCARD`: can be used in order to abort a transaction. No commands are executed and the state of the connection is restored to normal.
- `ROLLBACK`: Changes are actually written, lock is acquired, WAL is created. Changes is marked **abort** when the `ROLLBACK` is called.
- Example: 
```sh
> SET foo 1
OK
> MULTI
OK
> INCR foo
QUEUED
> DISCARD
OK
> GET foo
"1"
```

#### `WATCH`
- `WATCH` is used to provide Check-and-set (CAS). 
- Key is modified before `EXEC` command &rArr; The whole transaction is aborted & `EXEC` return `null` for inform the transaction failed.
- `WATCH` makes `EXEC` conditional

> Read mode here: [Redis::Transaction](https://redis.io/docs/latest/develop/using-commands/transactions/)

### Lua Scripts
- I have not dive deep into this, but basically we sent a whole lua script to redis server and it will run it atomically. 
- We can use if the opertions is complicated, for example we have to get it, check the current value with some conditions and then decided to set it or not. 
- Not to use it when the operation need to call another service because since the script is sent it wont know about our other services. 

### Which one is better? Why? 
- For most cases, Lua is better. Just except the third bullet above
- The `WATCH` just the key, if it is changed, the transaction will be aborted when `EXEC` is called and we have to handle the retry mechanism. With Lua Script, Redis treat a script as a singular operation, since Redis is single threaded, every is serialized. So basically, it will success and no retry mechanism required. 
- Reduce Network latency (i guess), since all operation is proceed inside Redis server instead of we call Get, Set from our application. 
 
### Why we choose to use the `WATCH/MULTI/EXEC`
Here is a current workflow of my `computeIfPresent` method:
```
public boolean computeIfPresent(K key, V value, Predicate<V> skipIf) {
    retry-loop {
        open Redis's AbstractTransaction atx {
            WATCH the key
            
            Get currentValue
            if (skipIf(currentValue)) {
                return false // Condition is satisfied, nothing to compute.
            }

            MULTI
            SET
            EXEC
        }
    }
}
```

I'm using WATCH/MULTI/EXEC because I see it more easy to implement and dont have to work with lua (as I said, I havent dive deep into Lua script). But be honest, in this case Lua script is more efficient thatn W/M/E. The `Predicate<V>` is just for `See if the value should be computed or not`. Expecting we just need some values and condition like this call:
```java
public boolean checkAndUpdate(String key, Job value) {
    boolean isSaved = jobStorage.computeIfPresent(key, value, (v) -> FINAL_STATES.contains(v.status()));
    return isSaved;
}
```

So the idea is to parameterize the Lua Script, receiving args that we can pass into for controlling the condition. 
