package com.dhbw.strand_pepperstudies_studien_CoronaCheck;

import static com.airbnb.lottie.L.TAG;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ApproachHumanBuilder;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.ExplorationMap;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.Localize;
import com.aldebaran.qi.sdk.object.actuation.LocalizeAndMap;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
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

import java.util.List;



public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    // The QiContext provided by the QiSDK.
    private LocalizeAndMap localizeAndMap;
    private Future<Void> localizationAndMapping;
    private ExplorationMap explorationMap;
    private Localize localize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QiSDK.register(this, this);

    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {

        // Store the provided QiContext.

        startMapping(qiContext);
        // Get the HumanAwareness service from the QiContext.


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
                    break;
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