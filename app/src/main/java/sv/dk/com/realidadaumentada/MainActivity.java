package sv.dk.com.realidadaumentada;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.rendering.ExternalRendering;
import com.wikitude.tracker.ImageTarget;
import com.wikitude.tracker.ImageTracker;
import com.wikitude.tracker.ImageTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import sv.dk.com.realidadaumentada.rendering.external.CustomSurfaceView;
import sv.dk.com.realidadaumentada.rendering.external.Driver;
import sv.dk.com.realidadaumentada.rendering.external.GLRenderer;
import sv.dk.com.realidadaumentada.rendering.external.StrokedRectangle;
import sv.dk.com.realidadaumentada.util.DropDownAlert;

public class MainActivity extends AppCompatActivity implements ImageTrackerListener, ExternalRendering {

    private static final String TAG = "SimpleImageTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;

    private DropDownAlert dropDownAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        startupConfiguration.setCameraResolution(CameraSettings.CameraResolution.AUTO);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);
        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/magazine.wtc");

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 5);

                wikitudeSDK.getTrackerManager().createImageTracker(targetCollectionResource, this, null);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Scan Target #1 (surfer):");
        dropDownAlert.addImages("surfer.png");
        dropDownAlert.setTextWeight(0.5f);
        dropDownAlert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        customSurfaceView.onPause();
//        driver.stop();
        wikitudeSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wikitudeSDK.onDestroy();
    }

    @Override
    public void onRenderExtensionCreated(RenderExtension renderExtension) {
        glRenderer = new GLRenderer(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);
        setContentView(customSurfaceView);
    }

    @Override
    public void onTargetsLoaded(ImageTracker imageTracker) {
        Log.v(TAG, "Image tracker loaded");
    }

    @Override
    public void onErrorLoadingTargets(ImageTracker imageTracker, WikitudeError wikitudeError) {
        Log.v(TAG, "Unable to load image tracker. Reason: " + wikitudeError.getMessage());
    }

    @Override
    public void onImageRecognized(ImageTracker imageTracker, ImageTarget imageTarget) {
        Log.v(TAG, "Recognized target " + imageTarget.getName());
        dropDownAlert.dismiss();

        StrokedRectangle strokedRectangle = new StrokedRectangle(StrokedRectangle.Type.STANDARD);
        glRenderer.setRenderablesForKey(imageTarget.getName() + imageTarget.getUniqueId(), strokedRectangle, null);
    }

    @Override
    public void onImageTracked(ImageTracker imageTracker, ImageTarget imageTarget) {
        StrokedRectangle strokedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey(imageTarget.getName() + imageTarget.getUniqueId());

        if (strokedRectangle != null) {
            strokedRectangle.viewMatrix = imageTarget.getViewMatrix();

            strokedRectangle.setXScale(imageTarget.getTargetScale().x);
            strokedRectangle.setYScale(imageTarget.getTargetScale().y);
        }
    }

    @Override
    public void onImageLost(ImageTracker imageTracker, ImageTarget imageTarget) {
        Log.v(TAG, "Lost target " + imageTarget.getName());
        glRenderer.removeRenderablesForKey(imageTarget.getName() + imageTarget.getUniqueId());
    }

    @Override
    public void onExtendedTrackingQualityChanged(ImageTracker imageTracker, ImageTarget imageTarget, int i, int i1) {

    }
}
