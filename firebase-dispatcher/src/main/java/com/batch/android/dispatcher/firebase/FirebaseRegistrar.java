package com.batch.android.dispatcher.firebase;

import android.content.Context;

import com.batch.android.BatchEventDispatcher;
import com.batch.android.eventdispatcher.DispatcherRegistrar;

/**
 * Firebase Registrar
 * The class will instantiate from the SDK using reflection
 * See the library {@link android.Manifest} for more information
 */
public class FirebaseRegistrar implements DispatcherRegistrar
{
    /**
     * Singleton instance
     */
    private static FirebaseDispatcher instance = null;

    /**
     * Singleton accessor
     * @param context Context used to initialize the dispatcher
     * @return Dispatcher instance
     */
    @Override
    public BatchEventDispatcher getDispatcher(Context context)
    {
        if (instance == null) {
            instance = new FirebaseDispatcher(context);
        }
        return instance;
    }
}
