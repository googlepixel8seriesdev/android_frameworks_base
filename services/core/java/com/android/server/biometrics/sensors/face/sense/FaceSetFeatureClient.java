/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2023 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.biometrics.sensors.face.sense;

import android.annotation.NonNull;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import com.android.server.biometrics.BiometricsProto;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.HalClientMonitor;

import java.util.ArrayList;
import java.util.function.Supplier;

import vendor.aospa.biometrics.face.ISenseService;

public class FaceSetFeatureClient extends HalClientMonitor<ISenseService> {

    private static final String TAG = "FaceSetFeatureClient";

    private final int mFeature;
    private final boolean mEnabled;
    private final byte[] mHardwareAuthToken;
    private final int mFaceId;

    FaceSetFeatureClient(@NonNull Context context, @NonNull Supplier<ISenseService> lazyDaemon,
            @NonNull IBinder token, @NonNull ClientMonitorCallbackConverter listener, int userId,
            @NonNull String owner, int sensorId,
            @NonNull BiometricLogger logger, @NonNull BiometricContext biometricContext,
            int feature, boolean enabled, byte[] hardwareAuthToken, int faceId) {
        super(context, lazyDaemon, token, listener, userId, owner, 0 /* cookie */, sensorId,
                logger, biometricContext, false /* isMandatoryBiometrics */);
        mFeature = feature;
        mEnabled = enabled;
        mFaceId = faceId;

        mHardwareAuthToken = (byte[]) hardwareAuthToken.clone();
    }

    @Override
    public void unableToStart() {
        try {
            getListener().onFeatureSet(false /* success */, mFeature);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to send error", e);
        }
    }

    @Override
    public void start(@NonNull ClientMonitorCallback callback) {
        super.start(callback);

        startHalOperation();
    }

    @Override
    protected void startHalOperation() {
        try {
            getFreshDaemon().setFeature(mFeature, mEnabled, mHardwareAuthToken, mFaceId);
            getListener().onFeatureSet(true, mFeature);
            mCallback.onClientFinished(this, true /* success */);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to set feature: " + mFeature + " to enabled: " + mEnabled, e);
            mCallback.onClientFinished(this, false /* success */);
        }
    }

    @Override
    public int getProtoEnum() {
        return BiometricsProto.CM_SET_FEATURE;
    }
}
