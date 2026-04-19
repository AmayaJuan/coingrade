package com.deltaforce.coingrade.ui.capture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Camera;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import android.graphics.Color;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.FrameLayout;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import com.deltaforce.coingrade.R;
import com.deltaforce.coingrade.databinding.FragmentCaptureBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureFragment extends Fragment {

    private FragmentCaptureBinding binding;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private Camera camera;
    private boolean isFlashOn = false;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private int photoCount = 0;
    private static final int TOTAL_PHOTOS = 6;
    private final List<Bitmap> capturedBitmaps = new ArrayList<>();

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && binding != null) startCamera();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCaptureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.previewRoundContainer.post(() -> {
            binding.previewRoundContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline outline) {
                    outline.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            binding.previewRoundContainer.setClipToOutline(true);
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
            startCamera();
        else
            permissionLauncher.launch(Manifest.permission.CAMERA);

        updateUI();
        binding.btnCapture.setOnClickListener(v -> takePhoto());
        binding.btnRestart.setOnClickListener(v -> restartPhotos());
        binding.btnFlash.setOnClickListener(v -> toggleFlash());

        setupTapToFocus();
    }

    private void setupTapToFocus() {
        binding.previewView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (camera == null) return false;

                MeteringPointFactory factory = binding.previewView.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(event.getX(), event.getY());
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build();

                camera.getCameraControl().startFocusAndMetering(action);
                return true;
            }
            return false;
        });
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            // Using a simple icon change to indicate state
            binding.btnFlash.setIconTint(ContextCompat.getColorStateList(requireContext(), 
                isFlashOn ? android.R.color.holo_orange_light : R.color.white));
        }
    }

    private void updateUI() {
        if (binding == null) return;
        
        boolean isObverse = photoCount < 3;
        int currentInSide = (photoCount % 3);
        
        binding.txtSideTitle.setText(isObverse ? R.string.photo_obverse : R.string.photo_reverse);
        String countText = (isObverse ? "Anverso" : "Reverso") + " - " + currentInSide + " / 3";
        binding.txtSideCount.setText(countText);

        // Update slots
        updateSlots(currentInSide);

        if (photoCount > 0) {
            binding.btnRestart.setVisibility(View.VISIBLE);
        } else {
            binding.btnRestart.setVisibility(View.GONE);
        }
    }

    private void updateSlots(int currentInSide) {
        binding.slot1.removeAllViews();
        binding.slot2.removeAllViews();
        binding.slot3.removeAllViews();

        int startIdx = (photoCount / 3) * 3;
        
        if (photoCount > startIdx) addThumbToSlot(binding.slot1, capturedBitmaps.get(startIdx));
        else addPlusToSlot(binding.slot1);

        if (photoCount > startIdx + 1) addThumbToSlot(binding.slot2, capturedBitmaps.get(startIdx + 1));
        else addPlusToSlot(binding.slot2);

        if (photoCount > startIdx + 2) addThumbToSlot(binding.slot3, capturedBitmaps.get(startIdx + 2));
        else addPlusToSlot(binding.slot3);
    }

    private void addThumbToSlot(FrameLayout slot, Bitmap bmp) {
        ImageView iv = new ImageView(requireContext());
        iv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageBitmap(bmp);
        iv.setClipToOutline(true);
        slot.addView(iv);
    }

    private void addPlusToSlot(FrameLayout slot) {
        ImageView iv = new ImageView(requireContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.CENTER;
        iv.setLayoutParams(lp);
        iv.setImageResource(android.R.drawable.ic_input_add);
        slot.addView(iv);
    }

    private void restartPhotos() {
        photoCount = 0;
        capturedBitmaps.clear();
        updateUI();
    }

    private void startCamera() {
        if (binding == null) return;

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                provider.unbindAll();
                camera = provider.bindToLifecycle(this, selector, preview, imageCapture, imageAnalysis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyzeImage(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        long sum = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 4;
        int count = 0;

        for (int y = centerY - radius; y < centerY + radius; y += 20) {
            for (int x = centerX - radius; x < centerX + radius; x += 20) {
                int index = y * width + x;
                if (index >= 0 && index < data.length) {
                    sum += (data[index] & 0xFF);
                    count++;
                }
            }
        }

        boolean isReady = count > 0 && (sum / count) > 80; // Simple light check for "framing"

        requireActivity().runOnUiThread(() -> {
            if (binding != null) {
                if (isReady) {
                    binding.ringOverlay.setBackgroundResource(R.drawable.coin_overlay_border);
                    binding.btnCapture.setEnabled(true);
                    binding.btnCapture.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_500));
                    binding.btnCapture.setText(R.string.take_photo);
                    binding.txtFramingStatus.setText(R.string.optimum);
                } else {
                    binding.ringOverlay.setBackgroundColor(Color.parseColor("#44FF0000"));
                    binding.btnCapture.setEnabled(false);
                    binding.btnCapture.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                    binding.btnCapture.setText(R.string.waiting_framing);
                    binding.txtFramingStatus.setText(R.string.center_coin);
                }
            }
        });
        image.close();
    }

    private void takePhoto() {
        if (imageCapture == null || binding == null) return;

        binding.btnCapture.setEnabled(false);

        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap bitmap = imageProxyToBitmap(image);
                        image.close();
                        
                        capturedBitmaps.add(bitmap);
                        photoCount++;

                        requireActivity().runOnUiThread(() -> {
                            if (photoCount >= TOTAL_PHOTOS) {
                                String name = binding.editCoinName.getText().toString().trim();
                                if (name.isEmpty()) name = getString(R.string.coin_default);
                                
                                Bundle b = new Bundle();
                                b.putString("coinName", name);
                                Navigation.findNavController(binding.getRoot())
                                        .navigate(R.id.action_capture_to_verification, b);
                            } else {
                                updateUI();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                        requireActivity().runOnUiThread(() -> binding.btnCapture.setEnabled(true));
                    }
                });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        Matrix matrix = new Matrix();
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
