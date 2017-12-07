package cl.cromer.ubb.attendance;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class SubjectListAdapter extends RecyclerView.Adapter<SubjectListAdapter.SubjectViewHolder> {

    private List<Subject> subjects;

    public SubjectListAdapter(List<Subject> subjects) {
        this.subjects = subjects;
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    @Override
    public SubjectViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_subject_list_card, viewGroup, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SubjectViewHolder subjectViewHolder, int i) {
        subjectViewHolder.majorName.setText(subjects.get(i).getMajorName());
        subjectViewHolder.majorCode.setText(String.valueOf(subjects.get(i).getMajorCode()));
        subjectViewHolder.majorId.setText(String.valueOf(subjects.get(i).getMajorId()));
        subjectViewHolder.subjectName.setText(subjects.get(i).getSubjectName());
        subjectViewHolder.subjectCode.setText(String.valueOf(subjects.get(i).getSubjectCode()));
        subjectViewHolder.subjectId.setText(String.valueOf(subjects.get(i).getSubjectId()));
        subjectViewHolder.itemView.setLongClickable(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void addSubject(Subject subject) {
        subjects.add(getItemCount(), subject);
        notifyItemInserted(getItemCount());
    }

    public void updateSubject(int index, Subject subject) {
        subjects.set(index, subject);
        notifyItemChanged(index);
    }

    public boolean hasSubject(Subject subject) {
        for (int i = 0; i < subjects.size(); i++) {
            if (subjects.get(i).getSubjectId() == subject.getSubjectId()) {
                return true;
            }
        }
        return false;
    }

    public Subject getSubject(int index) {
        return subjects.get(index);
    }

    public void deleteSubject(Subject subject) {
        subjects.remove(subject);
    }

    public static class SubjectViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;

        public TextView majorName;
        public TextView majorCode;
        public TextView majorId;
        public TextView subjectName;
        public TextView subjectCode;
        public TextView subjectId;

        public SubjectViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.subject_card_view);
            majorName = (TextView) itemView.findViewById(R.id.major_name);
            majorCode = (TextView) itemView.findViewById(R.id.major_code);
            majorId = (TextView) itemView.findViewById(R.id.major_id);
            subjectName = (TextView) itemView.findViewById(R.id.subject_name);
            subjectCode = (TextView) itemView.findViewById(R.id.subject_code);
            subjectId = (TextView) itemView.findViewById(R.id.subject_id);
        }
    }

}