package cl.cromer.ubb.attendance;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ClassListAdapter extends RecyclerView.Adapter<ClassListAdapter.ClassViewHolder> {

    private List<Class> classes;
    private Context context;

    public ClassListAdapter(List<Class> classes, Context context) {
        this.classes = classes;
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    @Override
    public ClassViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_class_list_card, viewGroup, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ClassViewHolder classViewHolder, int i) {
        classViewHolder.classDate.setText(String.valueOf(classes.get(i).getFormattedDate(context)));
        classViewHolder.classId.setText(String.valueOf(classes.get(i).getClassId()));
        classViewHolder.itemView.setLongClickable(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void addClass(Class classObject) {
        classes.add(getItemCount(), classObject);
        notifyItemInserted(getItemCount());
    }

    public void updateClass(int index, Class classObject) {
        classes.set(index, classObject);
        notifyItemChanged(index);
    }

    public Class getClass(int index) {
        return classes.get(index);
    }

    public void deleteClass(Class classObject) {
        classes.remove(classObject);
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView classDate;
        public TextView classId;

        public ClassViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.class_card_view);
            classDate = (TextView) itemView.findViewById(R.id.class_date);
            classId = (TextView) itemView.findViewById(R.id.class_id);
        }
    }

}