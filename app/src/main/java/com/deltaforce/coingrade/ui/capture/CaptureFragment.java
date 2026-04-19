package com.deltaforce.coingrade.ui.capture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import java.nio.ByteBuffer;
import android.graphics.Color;
import android.widget.Toast;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import com.deltaforce.coingrade.R;
import com.deltaforce.coingrade.data.HistoryRepository;
import com.deltaforce.coingrade.databinding.FragmentCaptureBinding;
import com.deltaforce.coingrade.model.GradeCatalog;
import com.deltaforce.coingrade.model.GradeOption;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureFragment extends Fragment {

    private FragmentCaptureBinding binding;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private boolean isCoinDetected = false;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private int photoCount = 0;
    private static final int TOTAL_PHOTOS = 6;

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
    }

    private void updateUI() {
        if (binding == null) return;
        
        int currentInSide = (photoCount % 3) + 1;
        if (photoCount < 3) {
            binding.txtInstructions.setText(getString(R.string.photo_obverse, currentInSide));
        } else {
            binding.txtInstructions.setText(getString(R.string.photo_reverse, currentInSide));
        }
        
        binding.btnCapture.setText(getString(R.string.take_photo, photoCount + 1));
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
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Analizador para detectar la moneda
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    // Aquí iría la lógica de TensorFlow Lite. 
                    // Simulamos detección si hay suficiente luz en el centro.
                    analyzeImage(image);
                });

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                provider.unbindAll();
                provider.bindToLifecycle(this, selector, preview, imageCapture, imageAnalysis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyzeImage(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        // Calculamos la varianza de los píxeles en el centro para detectar "textura"
        // Una moneda tiene muchos relieves y detalles, una superficie lisa no.
        long sum = 0;
        long sumSq = 0;
        int count = 0;
        
        // Saltamos píxeles para procesar rápido (solo el centro)
        int step = 10; 
        for (int i = 0; i < data.length; i += step) {
            int pixel = data[i] & 0xFF;
            sum += pixel;
            sumSq += (long) pixel * pixel;
            count++;
        }

        double avg = (double) sum / count;
        double variance = ((double) sumSq / count) - (avg * avg);

        // Umbral de detección: 
        // Si la varianza es muy baja (liso) o está muy oscuro/muy brillante, no hay moneda.
        boolean detected = variance > 400 && avg > 40 && avg < 220;

        requireActivity().runOnUiThread(() -> {
            if (binding != null) {
                if (detected) {
                    // Círculo Dorado si detecta moneda
                    binding.ringOverlay.setBackgroundResource(R.drawable.coin_overlay_border);
                    binding.btnCapture.setEnabled(photoCount < TOTAL_PHOTOS);
                    binding.btnCapture.setAlpha(1.0f);
                    if (!isCoinDetected) {
                        // Opcional: una pequeña vibración o cambio de texto
                        binding.txtInstructions.setTextColor(Color.parseColor("#FFD700"));
                    }
                } else {
                    // Círculo Rojo semi-transparente si no detecta moneda
                    binding.ringOverlay.setBackgroundColor(Color.parseColor("#44FF0000"));
                    binding.btnCapture.setEnabled(false);
                    binding.btnCapture.setAlpha(0.3f);
                    binding.txtInstructions.setTextColor(Color.WHITE);
                }
                isCoinDetected = detected;
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
                    public void onCaptureSuccess(@NonNull androidx.camera.core.ImageProxy image) {
                        image.close();
                        photoCount++;

                        if (photoCount >= TOTAL_PHOTOS) {
                            performAnalysis();
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                updateUI();
                                binding.btnCapture.setEnabled(true);
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                        requireActivity().runOnUiThread(() -> binding.btnCapture.setEnabled(true));
                    }
                });
    }

    private void performAnalysis() {
        if (binding == null) return;

        // Mostrar estado de "Analizando" en la UI
        requireActivity().runOnUiThread(() -> {
            binding.btnCapture.setEnabled(false);
            binding.txtInstructions.setText(getString(R.string.analyzing));
            binding.btnCapture.setText(getString(R.string.analyzing));
        });

        // Simulación de procesamiento de las 6 imágenes (2 segundos)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (binding == null) return;

            String name = "";
            if (binding.editCoinName.getText() != null)
                name = binding.editCoinName.getText().toString().trim();

            if (name.isEmpty())
                name = getString(R.string.coin_default);

            final String coinName = name;
            
            GradeOption g = GradeCatalog.randomGrade();
            HistoryRepository.add(requireContext(), coinName, g.code, g.vp);

            Bundle b = new Bundle();
            b.putString("coinName", coinName);
            b.putString("gradeCode", g.code);
            b.putString("gradeName", g.displayName);
            b.putInt("vp", g.vp);

            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_capture_to_result, b);
        }, 2500);
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