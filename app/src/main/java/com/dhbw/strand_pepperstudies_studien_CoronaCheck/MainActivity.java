package com.dhbw.strand_pepperstudies_studien_CoronaCheck;

import static com.airbnb.lottie.L.TAG;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ApproachHumanBuilder;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeBuilder;
import com.aldebaran.qi.sdk.builder.LookAtBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.ExplorationMap;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.LocalizationStatus;
import com.aldebaran.qi.sdk.object.actuation.Localize;
import com.aldebaran.qi.sdk.object.actuation.LocalizeAndMap;
import com.aldebaran.qi.sdk.object.actuation.LookAt;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.aldebaran.qi.sdk.object.human.Human;
import com.aldebaran.qi.sdk.object.humanawareness.ApproachHuman;
import com.aldebaran.qi.sdk.object.humanawareness.HumanAwareness;

import java.util.List;


public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    private Future<Void> localizationAndMapping;
    private HumanAwareness humanAwareness;
    private ExplorationMap explorationMap = null;
    private Future<Void> lookAtFuture;
    private LookAt lookAt;
    private String vacState = "";
    private QiContext qiContext;
    private FreeFrame targetFrame;

    private enum state {
        mapping,
        waitForMap,
        localaize,
        waitForLocalaize,
        idle,
        approachHuman,
        waitForQrCode,
        goToPosition,
        checkVacState,
        waitForPosition,
        lookAt,
        waitForLookAt
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

        ImageView dhbwLogo = findViewById(R.id.imageViewDhbw);
        ImageView redCross = findViewById(R.id.imageViewRedCross);
        ImageView checkMark = findViewById(R.id.imageViewCheckMark);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                dhbwLogo.setVisibility(View.VISIBLE);
                checkMark.setVisibility(View.INVISIBLE);
                redCross.setVisibility(View.INVISIBLE);

            }
        });

        _state = state.mapping;
        Mapping mapping;
        List<Human> humantoaproach = null;
        this.qiContext = qiContext;
        GoTo goTo = null;
        Actuation actuation;
        Frame robotFrame;
        explorationMap = null;
        Localize localize = null;

        while (true) {
            switch (_state) {
                case mapping:
                    LocalizeAndMap localizeAndMap = LocalizeAndMapBuilder.with(qiContext).build();

                    localizeAndMap.addOnStatusChangedListener(localizationStatus -> {
                        if (localizationStatus == LocalizationStatus.LOCALIZED) {
                            localizationAndMapping.requestCancellation();
                            explorationMap = localizeAndMap.dumpMap();
                            _state = state.localaize;
                        }
                    });
                    localizationAndMapping = localizeAndMap.async().run();

                    _state = state.waitForMap;
                    break;
                case waitForMap:
                    SystemClock.sleep(100);
                    Log.i(TAG, "waitforMap");
                    break;
                case localaize:
                    Log.i(TAG, "Localazing");
                    localize = LocalizeBuilder.with(qiContext)
                            .withMap(explorationMap)
                            .build();

                    localize.addOnStatusChangedListener(status -> {
                        if (status == LocalizationStatus.LOCALIZED) {
                            _state = state.idle;
                        }
                    });
                    localize.async().run();
                    _state = state.waitForLocalaize;

                case waitForLocalaize:
                    Log.i(TAG, "WaitForLocalazing");
                    SystemClock.sleep(500);
                    actuation = qiContext.getActuation();
                    robotFrame = actuation.robotFrame();
                    Vector3 vector = new Vector3(1,0,1.25);
                    Transform transform = TransformBuilder.create().fromTranslation(vector);
                    mapping = qiContext.getMapping();
                    targetFrame = mapping.makeFreeFrame();
                    targetFrame.update(robotFrame, transform, 0L);
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

                    ApproachHuman approachHuman = ApproachHumanBuilder.with(qiContext)
                            .withHuman(humantoaproach.get(0))
                            .build();

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
                                "Just hold your QR code in front of the Camera! You have 20 seconds.");

                        Say say = SayBuilder.with(qiContext)
                                .withPhrase(phrase)
                                .build();

                        say.run();
                        _state = state.waitForQrCode;
                    }
                    break;

                case waitForQrCode:

                    Thread T = new Thread(() -> {
                        TimeServer timeServer = new TimeServer();
                        vacState = timeServer.run();
                        Log.i(TAG,"running");
                    });
                    T.start();

                    int timeoutInt = 1;
                    boolean timeout = false;
                    while (T.isAlive() && !timeout) {
                        timeoutInt++;
                        if (timeoutInt == 20){
                            T.interrupt();
                            timeout = true;
                            Phrase phrase = new Phrase("I did not find any QR Code, please try again!");

                            Say say = SayBuilder.with(qiContext)
                                    .withPhrase(phrase)
                                    .build();

                            say.run();
                            _state = state.goToPosition;

                        }

                        Log.i(TAG,"is Alive");
                        SystemClock.sleep(1000);
                        Log.i(TAG,vacState + " State");
                        if (vacState != "") {
                            T.interrupt();
                            _state = state.checkVacState;
                        }
                    }
                    break;

                case checkVacState:

                    Log.i(TAG,vacState + " checkVacState");
                    if (vacState.equals("true")){

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                dhbwLogo.setVisibility(View.INVISIBLE);
                                checkMark.setVisibility(View.VISIBLE);
                                redCross.setVisibility(View.INVISIBLE);

                            }
                        });

                        Phrase phrase = new Phrase("Everything is fine! Thank you for your cooperation!");

                        Say say = SayBuilder.with(qiContext)
                                .withPhrase(phrase)
                                .build();

                        say.run();
                    }
                    if (vacState.equals("false")){
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                dhbwLogo.setVisibility(View.INVISIBLE);
                                checkMark.setVisibility(View.INVISIBLE);
                                redCross.setVisibility(View.VISIBLE);

                            }
                        });
                        Phrase phrase = new Phrase("Your QR code seems to be not valid, please leave the building immediately!");

                        Say say = SayBuilder.with(qiContext)
                                .withPhrase(phrase)
                                .build();

                        say.run();
                    }

                    vacState = "";
                    _state = state.goToPosition;
                    break;
                case goToPosition:
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            dhbwLogo.setVisibility(View.VISIBLE);
                            checkMark.setVisibility(View.INVISIBLE);
                            redCross.setVisibility(View.INVISIBLE);

                        }
                    });
                    Mapping mapping2 = qiContext.getMapping();
                    Frame mapFrame = mapping2.mapFrame();

                    Log.i(TAG, "ToPosition");
                    goTo = GoToBuilder.with(qiContext)
                            .withFrame(mapFrame)
                            .build();

                    Future<Void> goToFuture = goTo.async().run();

                    goToFuture.thenConsume(future -> {
                        if (future.isSuccess()) {
                            Log.i(TAG, "GoTo action finished with success.");
                            _state = state.lookAt;
                        } else if (future.hasError()) {
                            Log.e(TAG, "GoTo action finished with error.", future.getError());
                            _state = state.lookAt;
                        }
                    });
                    _state = state.waitForPosition;
                    break;
                case waitForPosition:
                    SystemClock.sleep(500);
                    Log.i(TAG, "WaitForPosition");
                    break;

                case lookAt:
                    Log.i(TAG, "LookAt");
                    lookAt = LookAtBuilder.with(qiContext)
                            .withFrame(targetFrame.frame())
                            .build();
                    lookAt.addOnStartedListener(() -> Log.i(TAG, "LookAt action started."));
                    lookAtFuture = lookAt.async().run();

                    lookAtFuture.thenConsume(future -> {
                        if (future.isSuccess()) {
                            Log.i(TAG, "LookAt action finished with success.");
                            _state = state.idle;
                        } else if (future.isCancelled()) {
                            Log.i(TAG, "LookAt action was cancelled.");
                            _state = state.idle;
                        } else {
                            Log.e(TAG, "LookAt action finished with error.", future.getError());
                        }
                    });

                    _state = state.waitForLookAt;
                    break;

                case waitForLookAt:
                    Log.i(TAG, "WaitForLookAt");
                    SystemClock.sleep(5000);
                    lookAtFuture.requestCancellation();
                    break;
            }
        }


    }

    @Override
    public void onRobotFocusLost() {
       this.qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {

    }



}

