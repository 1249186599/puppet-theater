package com.example.woodenpuppetdemo.content

import kotlin.math.*

/** Short local path around the cart, including clearance for the puppet's body. */
internal object CartApproach {
    private const val HALF=.24f
    fun route(start: WalkPoint,cart: WalkPoint,goal: WalkPoint): List<WalkPoint> {
        if(CargoLevel.distance(start,goal)<.012f) return listOf(goal)
        val r=HALF+.006f
        val nodes=listOf(start,goal,WalkPoint(cart.x-r,cart.z-r),WalkPoint(cart.x-r,cart.z+r),
            WalkPoint(cart.x+r,cart.z-r),WalkPoint(cart.x+r,cart.z+r))
        val distances=FloatArray(nodes.size){Float.POSITIVE_INFINITY};distances[0]=0f
        val previous=IntArray(nodes.size){-1};val seen=BooleanArray(nodes.size)
        repeat(nodes.size) {
            val u=nodes.indices.filter{!seen[it]}.minByOrNull{distances[it]} ?: return@repeat
            seen[u]=true
            for(v in nodes.indices) if(!seen[v] && !crossesCart(nodes[u],nodes[v],cart)) {
                val cost=distances[u]+CargoLevel.distance(nodes[u],nodes[v])
                if(cost<distances[v]) {distances[v]=cost;previous[v]=u}
            }
        }
        check(previous[1]>=0) { "No safe cart approach from $start to $goal" }
        val path=mutableListOf<WalkPoint>();var node=1
        while(node!=0) {path.add(nodes[node]);node=previous[node]}
        return path.reversed()
    }
    fun crossesCart(a: WalkPoint,b: WalkPoint,cart: WalkPoint): Boolean {
        var entry=0f;var exit=1f
        val starts=floatArrayOf(a.x-cart.x,a.z-cart.z)
        val deltas=floatArrayOf(b.x-a.x,b.z-a.z)
        for(i in 0..1) {
            if(abs(deltas[i])<.000001f) {
                if(abs(starts[i])>=HALF) return false
            } else {
                val t1=(-HALF-starts[i])/deltas[i];val t2=(HALF-starts[i])/deltas[i]
                entry=max(entry,min(t1,t2));exit=min(exit,max(t1,t2))
                if(entry>=exit) return false
            }
        }
        return entry<exit && exit>0f && entry<1f
    }
}
