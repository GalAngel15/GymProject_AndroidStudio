package com.example.gymproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymproject.R;
import com.example.gymproject.interfaces.OnPlanClickListener;
import com.example.gymproject.models.WorkoutPlan;

import java.util.List;

public class WorkoutPlanAdapter extends RecyclerView.Adapter<WorkoutPlanAdapter.ViewHolder> {
    private final Context context;
    private List<WorkoutPlan> workoutPlans;
    private OnPlanClickListener onPlanClickedListener, onPlanDeletedListener;

    public WorkoutPlanAdapter(Context context, List<WorkoutPlan> workoutPlans) {
        this.context = context;
        this.workoutPlans = workoutPlans;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_workout_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutPlan workoutPlan = workoutPlans.get(position);
        holder.tvWorkoutPlanName.setText(workoutPlan.getName());
        holder.tvLastDate.setText("Last Workout Date: " + workoutPlan.getLastDate());
        holder.tvTimesDone.setText("Times Done: " + workoutPlan.getTimesDone());
        holder.tvDescription.setText("Description: " + workoutPlan.getDescription());

        holder.itemView.setOnClickListener(v -> {
            onPlanClickedListener.onClick(workoutPlan);
        });
        holder.btnDeletePlan.setOnClickListener(v -> {
            onPlanDeletedListener.onClick(workoutPlan);
        });

    }
    public void setOnPlanClickListener(OnPlanClickListener listener) {
        this.onPlanClickedListener = listener;
    }

    @Override
    public int getItemCount() {
        return workoutPlans.size();
    }

    public void setOnPlanDeletedListener(OnPlanClickListener onPlanDeletedListener) {
        this.onPlanDeletedListener = onPlanDeletedListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvWorkoutPlanName;
        public TextView tvLastDate;
        public TextView tvTimesDone;
        public TextView tvDescription;
        public ImageButton btnDeletePlan;

        public ViewHolder(View itemView) {
            super(itemView);
            tvWorkoutPlanName = itemView.findViewById(R.id.workout_LBL_name);
            tvLastDate = itemView.findViewById(R.id.workout_LBL_last_date);
            tvTimesDone = itemView.findViewById(R.id.workout_LBL_times_done);
            tvDescription = itemView.findViewById(R.id.workout_LBL_description);
            btnDeletePlan = itemView.findViewById(R.id.btnDeletePlan);
        }
    }
}
