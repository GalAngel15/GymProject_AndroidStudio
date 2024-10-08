package com.example.gymproject.managers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymproject.R;
import com.example.gymproject.activities.PlanPageActivity;
import com.example.gymproject.adapters.CustomExerciseAdapter;
import com.example.gymproject.interfaces.OnExerciseLoadedListener;
import com.example.gymproject.models.BuiltExercise;
import com.example.gymproject.models.CustomExercise;
import com.example.gymproject.utilities.DatabaseUtils;
import com.example.gymproject.utilities.DialogUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;


public class WorkoutPlanManager implements SetsManager.OnRestFinishListener {
    private final FirebaseUser currentUser;
    private RecyclerView recyclerView;
    private CustomExerciseAdapter adapter;
    private final List<CustomExercise> exerciseList;
    private final Context context;
    private final String workoutPlanId;
    private final SetsManager workoutSetManager;

    public WorkoutPlanManager(Context context, FirebaseUser currentUser, String workoutPlanId) {
        this.currentUser = currentUser;
        this.context = context;
        this.exerciseList = new ArrayList<>();
        this.workoutPlanId = workoutPlanId;
        workoutSetManager = new SetsManager(this, (PlanPageActivity) context);
        initRecyclerView();
    }

    public void initRecyclerView() {
        this.recyclerView = ((PlanPageActivity) context).findViewById(R.id.myPlansRecyclerView);
        adapter = new CustomExerciseAdapter(context, exerciseList);

        adapter.setOnExerciseEditedListener(this::handleExerciseEdit);
        adapter.setOnExerciseDeletedListener(this::handleExerciseDelete);
        adapter.setOnDoneSetListener(workoutSetManager::startNewSet);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
    }

    public void loadAllUserExercises() {
        List<CustomExercise> tempList = new ArrayList<>();
        loadCustomExercisesList(tempList);
    }

    private void loadCustomExercisesList(List<CustomExercise> tempList) {
        String userId = currentUser.getUid();
        if (workoutPlanId.isEmpty()) {
            Log.e("WorkoutPlanManager", "Error: User ID is null or empty");
            return;
        }

        DatabaseUtils.loadUserWorkoutPlan(userId, workoutPlanId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    loadExercisesFromDataSnapshot(dataSnapshot, tempList);
                }
                loadUserWorkoutWarehousePlan(workoutPlanId, tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("WorkoutPlanManager", "Error loading custom exercises: " + databaseError.getMessage());
            }
        });
    }

    private void loadUserWorkoutWarehousePlan(String workoutPlanId, List<CustomExercise> tempList) {
        DatabaseUtils.loadUserWorkoutWarehousePlan(currentUser.getUid(), workoutPlanId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int totalItems = (int) dataSnapshot.getChildrenCount();
                    int[] loadedItems = {0};

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        CustomExercise exercise = snapshot.getValue(CustomExercise.class);
                        if (exercise != null) {
                            DatabaseUtils.loadExerciseFromWarehouse(snapshot.getKey(), new OnExerciseLoadedListener() {
                                @Override
                                public void onExerciseLoaded(BuiltExercise builtExercise) {
                                    exercise.setMainMuscle(builtExercise.getMainMuscle());
                                    exercise.setName(builtExercise.getName());
                                    exercise.setImageUrl(builtExercise.getImageUrl());
                                    tempList.add(exercise);
                                    loadedItems[0]++;

                                    if (loadedItems[0] == totalItems) {
                                        // מיון ועדכון ה-RecyclerView לאחר טעינת כל הפריטים מהמחסן
                                        updateAndSortExercises(tempList);
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("WorkoutPlanManager", "Error loading exercise from warehouse: " + e.getMessage());
                                    loadedItems[0]++;

                                    if (loadedItems[0] == totalItems) {
                                        // מיון ועדכון ה-RecyclerView לאחר טעינת כל הפריטים מהמחסן
                                        updateAndSortExercises(tempList);
                                    }
                                }
                            });
                        } else {
                            loadedItems[0]++;
                            if (loadedItems[0] == totalItems) {
                                // מיון ועדכון ה-RecyclerView לאחר טעינת כל הפריטים מהמחסן
                                updateAndSortExercises(tempList);
                            }
                        }
                    }
                } else {
                    // אם אין פריטים במחסן, נעדכן ונמיין את ה-RecyclerView עם התרגילים המותאמים אישית בלבד
                    updateAndSortExercises(tempList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("WorkoutPlanManager", "Error loading warehouse exercises: " + databaseError.getMessage());
            }
        });
    }

    private void updateAndSortExercises(List<CustomExercise> tempList) {
        tempList.sort(Comparator.comparing(CustomExercise::getMainMuscle).thenComparing(CustomExercise::getName));
        if (!exerciseList.isEmpty()) {
            exerciseList.clear();
        }
        exerciseList.addAll(tempList);
        adapter.notifyDataSetChanged();
    }

    private void handleExerciseEdit(CustomExercise exercise) {
        DialogUtils.showEditExerciseDialog(context, exercise, updatedExercise ->
                DatabaseUtils.updateCustomExerciseInFirebase(currentUser.getUid(), workoutPlanId, updatedExercise, task -> {
                    if (task.isSuccessful()) {
                        updateExerciseInList(exercise, updatedExercise);
                    } else {
                        showToast("Failed to update exercise");
                    }
                }));
    }

    private void updateExerciseInList(CustomExercise oldExercise, CustomExercise updatedExercise) {
        int index = exerciseList.indexOf(oldExercise);
        if (index != -1) {
            exerciseList.set(index, updatedExercise);
            adapter.notifyItemChanged(index);
            showToast("Exercise updated");
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void handleExerciseDelete(CustomExercise exercise) {
        DialogUtils.showDeleteExerciseDialog(context, exercise, updatedExercise ->
                DatabaseUtils.deleteExerciseFromFirebase(currentUser.getUid(), workoutPlanId, updatedExercise, task -> {
                    if (task.isSuccessful()) {
                        removeExerciseFromList(exercise);
                    } else {
                        showToast("Failed to delete exercise");
                    }
                }));
    }

    private void loadExercisesFromDataSnapshot(DataSnapshot dataSnapshot, List<CustomExercise> exerciseList) {
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            CustomExercise exercise = snapshot.getValue(CustomExercise.class);
            if (exercise != null) {
                exerciseList.add(exercise);
            }
        }
    }

    private void removeExerciseFromList(CustomExercise exercise) {
        int index = exerciseList.indexOf(exercise);
        if (index != -1) {
            exerciseList.remove(index);
            adapter.notifyItemRemoved(index);
            showToast("Exercise deleted");
        }
    }

    @Override
    public void onSetCompleted(int setCount, CustomExercise customExercise) {
        int setsRemaining = customExercise.getSets() - setCount;
        Toast.makeText(context, "Set " + setCount + " completed! " + setsRemaining + " sets remaining.", Toast.LENGTH_SHORT).show();

        if (setsRemaining <= 0) {
            Toast.makeText(context, "Exercise completed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRestFinished() {
        // השמעת צליל בסיום הזמן
        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.ouchsound);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == AppCompatActivity.RESULT_OK) {
            onRestFinished();
        }
    }
}
