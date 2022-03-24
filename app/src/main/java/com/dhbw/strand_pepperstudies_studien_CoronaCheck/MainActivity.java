package com.dhbw.strand_pepperstudies_studien_CoronaCheck;

import static android.graphics.ImageFormat.YUV_420_888;
import static android.graphics.ImageFormat.YUV_422_888;
import static android.graphics.ImageFormat.YUV_444_888;
import static com.airbnb.lottie.L.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ApproachHumanBuilder;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TakePictureBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.ExplorationMap;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.LocalizationStatus;
import com.aldebaran.qi.sdk.object.actuation.Localize;
import com.aldebaran.qi.sdk.object.actuation.LocalizeAndMap;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.camera.TakePicture;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.human.AttentionState;
import com.aldebaran.qi.sdk.object.human.EngagementIntentionState;
import com.aldebaran.qi.sdk.object.human.ExcitementState;
import com.aldebaran.qi.sdk.object.human.Gender;
import com.aldebaran.qi.sdk.object.human.Human;
import com.aldebaran.qi.sdk.object.human.PleasureState;
import com.aldebaran.qi.sdk.object.human.SmileState;
import com.aldebaran.qi.sdk.object.humanawareness.ApproachHuman;
import com.aldebaran.qi.sdk.object.humanawareness.HumanAwareness;
import com.aldebaran.qi.sdk.object.image.EncodedImage;
import com.aldebaran.qi.sdk.object.image.EncodedImageHandle;
import com.aldebaran.qi.sdk.object.image.TimestampedImageHandle;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    // The QiContext provided by the QiSDK.
    private LocalizeAndMap localizeAndMap;
    private Future<Void> localizationAndMapping;
    private Localize localize;
    private HumanAwareness humanAwareness;
    private ExplorationMap explorationMap = null;

    private enum state {
        mapping,
        waitForMap,
        localaize,
        waitForLocalaize,
        idle,
        approachHuman,
        waitForQrCode,
        goToPosition,
        waitForPosition
    }

    private state _state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QiSDK.register(this, this);

    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {

        // Store the provided QiContext.
        _state = state.mapping;
        List<Human> humantoaproach = null;
        GoTo goTo = null;
        explorationMap = null;
        Localize localize = null;

        while (true) {
            switch (_state) {
                case mapping:
                    LocalizeAndMap localizeAndMap = LocalizeAndMapBuilder.with(qiContext).build();

                    localizeAndMap.addOnStatusChangedListener(localizationStatus -> {
                        if (localizationStatus == LocalizationStatus.LOCALIZED) {
                            localizationAndMapping.requestCancellation();
                            // Dump the map for future use by a Localize action.
                            explorationMap = localizeAndMap.dumpMap();
                            _state = state.localaize;
                        }
                    });
                    localizationAndMapping = localizeAndMap.async().run();

                    _state = state.waitForMap;
                    break;
                case waitForMap:
                    SystemClock.sleep(500);
                    Log.i(TAG, "waitforMap");
                    break;
                case localaize:
                    Log.i(TAG, "Localazing");
                    localize = LocalizeBuilder.with(qiContext)
                            .withMap(explorationMap)
                            .build();

                    localize.addOnStatusChangedListener(status -> {
                        if (status == LocalizationStatus.LOCALIZED) {
                            // Dump the map for future use by a Localize action.
                            _state = state.idle;
                        }
                    });
                    localize.async().run();
                    _state = state.waitForLocalaize;

                case waitForLocalaize:
                    Log.i(TAG, "WaitForLocalazing");
                    SystemClock.sleep(500);
                break;
                case idle:
                    Log.i(TAG, "Idle");
                    SystemClock.sleep(500);
                    humanAwareness = qiContext.getHumanAwareness();
                    humantoaproach = humanAwareness.getHumansAround();
                    if (!humantoaproach.isEmpty()) {
                        Log.i(TAG, "notEmpty");
                        _state = state.approachHuman;
                    }
                    break;
                case approachHuman:
                    Log.i(TAG, "ApproachHuman");
                    retrieveCharacteristics(humantoaproach);
                    ApproachHuman approachHuman = ApproachHumanBuilder.with(qiContext)
                            .withHuman(humantoaproach.get(0))
                            .build();

                    AtomicBoolean test = new AtomicBoolean(false);


                    approachHuman.addOnStartedListener(()-> {
                        Log.i(TAG, "Approach started!");
                    });

                    Future<Void> approach = approachHuman.async().run();

                    if(approach.hasError())
                    {
                        _state = state.goToPosition;
                    }
                    if(approach.isDone()){
                        Phrase phrase = new Phrase("Please show me your vaccination certificate. " +
                                "Just hold your QR code in front of the Camera on my head!");

                        Say say = SayBuilder.with(qiContext)
                                .withPhrase(phrase)
                                .build();

                        say.run();

                        _state = state.waitForQrCode;
                    }
                    break;

                case waitForQrCode:
                    Log.i(TAG, "Wait 10 sec for Qr code");
                    Holder holder = HolderBuilder.with(qiContext)
                            .withAutonomousAbilities(AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                                                        AutonomousAbilitiesType.BASIC_AWARENESS)
                            .build();
                    holder.async().hold();
                    SystemClock.sleep(10000);
                    holder.async().release();
                    _state = state.goToPosition;
                    break;
                case goToPosition:
                    Mapping mapping = qiContext.getMapping();
                    Frame mapFrame = mapping.mapFrame();
                    Log.i(TAG, "ToPosition");
                    goTo = GoToBuilder.with(qiContext)
                            .withFrame(mapFrame)
                            .build();

                    Future<Void> goToFuture = goTo.async().run();

                    goToFuture.thenConsume(future -> {
                        if (future.isSuccess()) {
                            Log.i(TAG, "GoTo action finished with success.");
                            _state = state.idle;
                        } else if (future.hasError()) {
                            Log.e(TAG, "GoTo action finished with error.", future.getError());
                            _state = state.idle;
                        }
                    });
                    _state = state.waitForPosition;
                    break;
                case waitForPosition:
                    SystemClock.sleep(500);
                    Log.i(TAG, "WaitForPosition");
                    break;
            }
        }


    }

    @Override
    public void onRobotFocusLost() {
       // this.qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        // Nothing here.
    }

    private void startMapping(QiContext qiContext) {
        // Create a LocalizeAndMap action.
        localizeAndMap = LocalizeAndMapBuilder.with(qiContext).build();

        // Add an on status changed listener on the LocalizeAndMap action to know when the robot has mapped his environment.
        localizeAndMap.addOnStatusChangedListener(status -> {
            switch (status) {
                case LOCALIZED:
                    // Dump the ExplorationMap.
                    explorationMap = localizeAndMap.dumpMap();

                    Log.i(TAG, "Robot has mapped his environment.");

                    // Cancel the LocalizeAndMap action.
                    localizationAndMapping.requestCancellation();
                    break;

            }
        });

        Log.i(TAG, "Mapping...");

        // Execute the LocalizeAndMap action asynchronously.
        localizationAndMapping = localizeAndMap.async().run();

        // Add a lambda to the action execution.
        localizationAndMapping.thenConsume(future -> {
            if (future.hasError()) {
                Log.e(TAG, "LocalizeAndMap action finished with error.", future.getError());
            } else if (future.isCancelled()) {
                startLocalizing(qiContext);
                // The LocalizeAndMap action has been cancelled.
            }
        });
    }

    private void startLocalizing(QiContext qiContext) {
        // Create a Localize action.
        localize = LocalizeBuilder.with(qiContext)
                .withMap(explorationMap)
                .build();

        // Add an on status changed listener on the Localize action to know when the robot is localized in the map.
        localize.addOnStatusChangedListener(status -> {
            switch (status) {
                case LOCALIZED:
                    Log.i(TAG, "Robot is localized.");

                    humanAwareness = qiContext.getHumanAwareness();
                    List<Human> humantoaproach = humanAwareness.getHumansAround();

                    _state = state.idle;
                    while(true) {
                        SystemClock.sleep(500);
                        humanAwareness = qiContext.getHumanAwareness();
                        humantoaproach = humanAwareness.getHumansAround();

                        switch (_state){

                            case idle:
                                Log.i(TAG, "Idle");
                                humanAwareness = qiContext.getHumanAwareness();
                                humantoaproach = humanAwareness.getHumansAround();
                                if (!humantoaproach.isEmpty()) {
                                    Log.i(TAG, "notEmpty");
                                    _state = state.approachHuman;
                                }
                                break;


                            case approachHuman:
                                Log.i(TAG, "ApproachHuman");
                                retrieveCharacteristics(humantoaproach);
                                ApproachHuman approachHuman = ApproachHumanBuilder.with(qiContext)
                                        .withHuman(humantoaproach.get(0))
                                        .build();
                                approachHuman.async().run();

                                if(approachHuman.async().run().hasError()){
                                    Mapping mapping = qiContext.getMapping();
                                    Frame mapFrame = mapping.mapFrame();
                                    Log.i(TAG, "ToPosition");
                                    GoTo goTo = GoToBuilder.with(qiContext)
                                            .withFrame(mapFrame)
                                            .build();
                                    goTo.async().run();
                                    // Run the action synchronously.

                                }


                                _state = state.idle;
                                break;

                            case goToPosition:
                                break;
                        }


                    }

            }
        });

        Log.i(TAG, "Localizing...");

        // Execute the Localize action asynchronously.
        Future<Void> localization = localize.async().run();

        // Add a lambda to the action execution.
        localization.thenConsume(future -> {
            if (future.hasError()) {
                Log.e(TAG, "Localize action finished with error.", future.getError());
            }
        });
    }



    private void retrieveCharacteristics(final List<Human> humans) {
        // Here we will retrieve the people characteristics.
        for (int i = 0; i < humans.size(); i++) {
            // Get the human.
            Human human = humans.get(i);

            // Get the characteristics.
            Integer age = human.getEstimatedAge().getYears();
            Gender gender = human.getEstimatedGender();
            PleasureState pleasureState = human.getEmotion().getPleasure();
            ExcitementState excitementState = human.getEmotion().getExcitement();
            EngagementIntentionState engagementIntentionState = human.getEngagementIntention();
            SmileState smileState = human.getFacialExpressions().getSmile();
            AttentionState attentionState = human.getAttention();

            // Display the characteristics.
            Log.i(TAG, "----- Human " + i + " -----");
            Log.i(TAG, "Age: " + age + " year(s)");
            Log.i(TAG, "Gender: " + gender);
            Log.i(TAG, "Pleasure state: " + pleasureState);
            Log.i(TAG, "Excitement state: " + excitementState);
            Log.i(TAG, "Engagement state: " + engagementIntentionState);
            Log.i(TAG, "Smile state: " + smileState);
            Log.i(TAG, "Attention state: " + attentionState);
        }
    }




}