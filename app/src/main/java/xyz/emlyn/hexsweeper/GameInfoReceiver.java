package xyz.emlyn.hexsweeper;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class GameInfoReceiver extends BroadcastReceiver {

    ConstraintLayout fullScreenLayout;
    Activity activity;

    public GameInfoReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()) {
            case "xyz.emlyn.hexsweeper.GAME_WIN":
                LayoutInflater winInflater = LayoutInflater.from(context);
                ConstraintLayout winOverlayCL = (ConstraintLayout) winInflater.inflate(R.layout.game_end_overlay, fullScreenLayout, false);
                final int winOverlayID = View.generateViewId();
                winOverlayCL.setId(winOverlayID);

                winOverlayCL.setAlpha(0);
                //Capture clicks
                winOverlayCL.setOnClickListener(view -> {});

                        winOverlayCL.findViewById(R.id.overlayBackground).setBackgroundColor(context.getColor(R.color.win_overlay));
                ((TextView) winOverlayCL.findViewById(R.id.gameEndState)).setText(context.getString(R.string.win));

                fullScreenLayout.addView(winOverlayCL);

                ValueAnimator winOpacityAnim = ValueAnimator.ofFloat(0, 1);
                winOpacityAnim.setDuration(500); //ms
                winOpacityAnim.addUpdateListener(valueAnimator -> {
                    float newAlpha = (float) valueAnimator.getAnimatedValue();
                    if (newAlpha == 1) {
                        //Finish after 500ms
                        Handler finishHandler = new Handler();
                        finishHandler.postDelayed(() -> activity.finish(), 1250);
                    } else {
                        fullScreenLayout.findViewById(winOverlayID).setAlpha(newAlpha);
                    }
                });
                winOpacityAnim.start();
                break;

            case "xyz.emlyn.hexsweeper.GAME_LOSS":
                LayoutInflater lossInflater = LayoutInflater.from(context);
                ConstraintLayout lossOverlayCL = (ConstraintLayout) lossInflater.inflate(R.layout.game_end_overlay, fullScreenLayout, false);
                final int lossOverlayID = View.generateViewId();
                lossOverlayCL.setId(lossOverlayID);

                lossOverlayCL.setAlpha(0);
                //Capture clicks
                lossOverlayCL.setOnClickListener(view -> {});

                        lossOverlayCL.findViewById(R.id.overlayBackground).setBackgroundColor(context.getColor(R.color.loss_overlay));
                ((TextView) lossOverlayCL.findViewById(R.id.gameEndState)).setText(context.getString(R.string.loss));

                try {
                    fullScreenLayout.addView(lossOverlayCL);
                } catch (Exception ignored) { break; }

                ValueAnimator lossOpacityAnim = ValueAnimator.ofFloat(0, 1);
                lossOpacityAnim.setDuration(500); //ms
                lossOpacityAnim.addUpdateListener(valueAnimator -> {
                    float newAlpha = (float) valueAnimator.getAnimatedValue();
                    if (newAlpha == 1) {
                        //Finish after 500ms
                        Handler finishHandler = new Handler();
                        finishHandler.postDelayed(() -> activity.finish(), 1250);
                    } else {
                        fullScreenLayout.findViewById(lossOverlayID).setAlpha(newAlpha);
                    }
                });
                lossOpacityAnim.start();
                break;

            case "xyz.emlyn.hexsweeper.CHANGE_HEX":
                int deltaHex = intent.getIntExtra("deltaHex", -999);
                if (deltaHex != -999) {
                    TextView numHexTV = fullScreenLayout.findViewById(R.id.hexTV);
                    int existingHex = Integer.parseInt(numHexTV.getText().toString());
                    numHexTV.setText(String.valueOf((existingHex + deltaHex)));
                }
                break;

            case "xyz.emlyn.hexsweeper.DEC_MINE":
                try {
                    TextView nMineTV = fullScreenLayout.findViewById(R.id.mineTV);
                    int existingMine = Integer.parseInt(nMineTV.getText().toString());
                    nMineTV.setText(String.valueOf(existingMine - 1));
                } catch (NullPointerException ignored) {}

        }
    }
}
