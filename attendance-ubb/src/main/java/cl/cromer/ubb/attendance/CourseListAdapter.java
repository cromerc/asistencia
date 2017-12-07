package cl.cromer.ubb.attendance;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class CourseListAdapter extends RecyclerView.Adapter<CourseListAdapter.CourseViewHolder> {

    private List<Course> courses;

    public CourseListAdapter(List<Course> courses) {
        this.courses = courses;
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    @Override
    public CourseViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_course_list_card, viewGroup, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CourseViewHolder courseViewHolder, int i) {
        courseViewHolder.courseYear.setText(String.valueOf(courses.get(i).getYear()));
        courseViewHolder.courseSemester.setText(String.valueOf(courses.get(i).getCourseSemester()));
        courseViewHolder.courseSection.setText(String.valueOf(courses.get(i).getCourseSection()));
        courseViewHolder.courseId.setText(String.valueOf(courses.get(i).getCourseId()));
        courseViewHolder.itemView.setLongClickable(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void addCourse(Course course) {
        courses.add(getItemCount(), course);
        notifyItemInserted(getItemCount());
    }

    public void updateCourse(int index, Course course) {
        courses.set(index, course);
        notifyItemChanged(index);
    }

    public Course getCourse(int index) {
        return courses.get(index);
    }

    public void deleteCourse(Course course) {
        courses.remove(course);
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public TextView courseYear;
        public TextView courseSemester;
        public TextView courseSection;
        public TextView courseId;

        public CourseViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.course_card_view);
            courseYear = (TextView) itemView.findViewById(R.id.course_year);
            courseSemester = (TextView) itemView.findViewById(R.id.course_semester);
            courseSection = (TextView) itemView.findViewById(R.id.course_section);
            courseId = (TextView) itemView.findViewById(R.id.course_id);
        }
    }

}