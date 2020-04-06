package wseemann.media.romote.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.model.Device;
import com.jaku.parser.DeviceParser;
import com.jaku.request.KeypressRequest;
import com.jaku.request.QueryDeviceInfoRequest;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import wseemann.media.romote.R;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.tasks.RxRequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;
import wseemann.media.romote.view.RepeatingImageButton;

/**
 * Created by wseemann on 6/19/16.
 */
public class RemoteFragment extends Fragment {

    private static final String TAG = "RemoteFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remote, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        /*mVoiceSearcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySpeechRecognizer();
            }
        });
        mVoiceSearcButton.requestFocus();*/

        linkButton(KeypressKeyValues.BACK, R.id.back_button);
        linkRepeatingRemoteButton(KeypressKeyValues.UP, R.id.up_button);
        linkButton(KeypressKeyValues.HOME, R.id.home_button);

        linkRepeatingRemoteButton(KeypressKeyValues.LEFT, R.id.left_button);
        linkButton(KeypressKeyValues.SELECT, R.id.ok_button);
        linkRepeatingRemoteButton(KeypressKeyValues.RIGHT, R.id.right_button);

        linkButton(KeypressKeyValues.INTANT_REPLAY, R.id.instant_replay_button);
        linkRepeatingRemoteButton(KeypressKeyValues.DOWN, R.id.down_button);
        linkButton(KeypressKeyValues.INFO, R.id.info_button);

        linkButton(KeypressKeyValues.REV, R.id.rev_button);
        linkButton(KeypressKeyValues.PLAY, R.id.play_button);
        linkButton(KeypressKeyValues.FWD, R.id.fwd_button);

        linkButton(KeypressKeyValues.VOLUME_MUTE, R.id.mute_button);
        linkButton(KeypressKeyValues.VOLUME_DOWN, R.id.volume_down_button);
        linkButton(KeypressKeyValues.VOLUME_UP, R.id.volume_up_button);

        ImageButton keyboardButton = getView().findViewById(R.id.keyboard_button);
        keyboardButton.setOnClickListener(view -> {
            TextInputDialog fragment = new TextInputDialog();
            fragment.show(RemoteFragment.this.getFragmentManager(), TextInputDialog.class.getName());
        });

        ImageButton powerButton = getView().findViewById(R.id.power_button);
        powerButton.setOnClickListener(view -> {
            obtainPowerMode();
        });

        getView().findViewById(R.id.remote_dpad_controls).bringToFront();
        updateVolumeControls();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.UPDATE_DEVICE_BROADCAST);
        getActivity().registerReceiver(mUpdateReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUpdateReceiver);
    }

    private void linkRepeatingRemoteButton(final KeypressKeyValues keypressKeyValue, int id) {
        RepeatingImageButton button = getView().findViewById(id);

        button.setOnClickListener(view -> {
            performKeypress(keypressKeyValue);
        });

        button.setRepeatListener(new RepeatingImageButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long duration, int repeatcount) {
                performKeypress(keypressKeyValue);
            }
        }, 400);
    }

    private void linkButton(final KeypressKeyValues keypressKeyValue, int id) {
        ImageButton button = getView().findViewById(id);

        button.setOnClickListener(view -> {
            performKeypress(keypressKeyValue);

            if (id == R.id.back_button ||
                    id == R.id.home_button ||
                    id == R.id.ok_button) {
                getContext().sendBroadcast(new Intent(Constants.UPDATE_DEVICE_BROADCAST));
            }
        });
    }

    private void performRequest(final JakuRequest request, final RokuRequestTypes rokuRequestType) {
        Observable.fromCallable(new RxRequestTask(getContext().getApplicationContext(), request, rokuRequestType))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> { });
    }

    private void obtainPowerMode() {
        String url = CommandHelper.getDeviceURL(getActivity());

        QueryDeviceInfoRequest queryActiveAppRequest = new QueryDeviceInfoRequest(url);
        JakuRequest request = new JakuRequest(queryActiveAppRequest, new DeviceParser());

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                performPowerAction((Device) result.mResultValue);
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                Log.d("TAG", result.mException.getMessage());
            }
        }).execute(RokuRequestTypes.query_device_info);
    }

    private void performPowerAction(final Device device) {
        if (device == null) {
            return;
        }

        if (device.getPowerMode().equals("PowerOn")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.power_dialog_title);
            builder.setMessage(R.string.power_dialog_message);
            builder.setCancelable(true);
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    performKeypress(KeypressKeyValues.POWER_OFF);
                }
            });

            Dialog dialog = builder.create();
            dialog.show();
        } else {
            performKeypress(KeypressKeyValues.POWER_ON);
        }
    }

    private void performKeypress(KeypressKeyValues keypressKeyValue) {
        /*try {
            Device device = PreferenceUtils.getConnectedDevice(getContext());
            device.setHost("http://1234");
            DBUtils.updateDevice(getContext(), device);
        } catch (Exception ex) {

        }*/

        String url = CommandHelper.getDeviceURL(getActivity());

        KeypressRequest keypressRequest = new KeypressRequest(url, keypressKeyValue.getValue());
        JakuRequest request = new JakuRequest(keypressRequest, null);

        performRequest(request, RokuRequestTypes.keypress);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.remote_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_cancel) {
            getActivity().finish();
            return true;
        }

        return false;
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Do something with spokenText
            //mTextBox.setText(spokenText);
            //Toast.makeText(getActivity(), spokenText, Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVolumeControls();
        }
    };

    private void updateVolumeControls() {
        try {
            Device device = PreferenceUtils.getConnectedDevice(getContext());

            if (device.getIsTv() != null) {
                boolean isTv = Boolean.valueOf(device.getIsTv());
                getView().findViewById(R.id.volume_controls).setVisibility(isTv ? View.VISIBLE : View.GONE);
            }

        } catch (Exception ex) {
            Log.e(TAG, "Error updating remote layout for newly connected device.");
        }
    }
}
