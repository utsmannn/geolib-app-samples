package com.utsman.geolibsample.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class CombinedLiveData<T, K, S>(
    source1: LiveData<T>,
    source2: LiveData<T>,
    source3: LiveData<K>,
    private val combine: (data1: T?, data2: T?, data3: K?) -> S
) : MediatorLiveData<S>() {

    private var data1: T? = null
    private var data2: T? = null
    private var data3: K? = null

    init {
        super.addSource(source1) {
            data1 = it
            value = combine(data1, data2, data3)
        }
        super.addSource(source2) {
            data2 = it
            value = combine(data1, data2, data3)
        }
        super.addSource(source3) {
            data3 = it
            value = combine(data1, data2, data3)
        }
    }

    override fun <T : Any?> addSource(source: LiveData<T>, onChanged: Observer<in T>) {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> removeSource(toRemote: LiveData<T>) {
        throw UnsupportedOperationException()
    }
}