package ru.ifmo.rain.lyubavina.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private List<String> getList(List<Student> students, Function<Student, String> function) {
        return students.stream().map(function).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getList(students, student -> student.getFirstName() + " " + student.getLastName());
    }


    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<>(getFirstNames(students));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(Student::compareTo).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(Comparator.comparing(Student::getLastName)
                .thenComparing(Student::getFirstName)
                .thenComparing(Student::compareTo)).collect(Collectors.toList());
    }


    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudents(students, student -> student.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudents(students, student -> student.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudents(students, student -> student.getGroup().equals(group));
    }


    private List<Student> findStudentsByGroupSortedById(Collection<Student> students, String group) {
        return sortStudentsById(students).stream().filter(student -> student.getGroup().equals(group)).collect(Collectors.toList());
    }


    private List<Student> findStudents(Collection<Student> students, Predicate<Student> predicate) {
        return sortStudentsByName(students).stream().filter(predicate).collect(Collectors.toList());
    }


    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors
                        .toMap(Student::getLastName, Student::getFirstName,   //key function, value function
                                (a, b) -> a.compareTo(b) <= 0 ? a : b));   //merge function for the same keys


    }


    private Stream<String> allGroupsStream(Collection<Student> students) {
        return students.stream().map(Student::getGroup).distinct();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroups(students, group -> new Group(group, findStudentsByGroup(students, group)));
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroups(students, group -> new Group(group, findStudentsByGroupSortedById(students, group)));
    }


    private List<Group> getGroups(Collection<Student> students, Function<String, Group> function) {
        return allGroupsStream(students).sorted(String::compareTo)
                .map(function).collect(Collectors.toList());
    }


    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupMain(students, group -> new Group(group, findStudentsByGroup(students, group)),
                (Group g) -> g.getStudents().size());
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupMain(students, group -> new Group(group, findStudentsByGroup(students, group)),
                (Group g) -> getDistinctFirstNames(g.getStudents()).size());

    }

    private String getLargestGroupMain(Collection<Student> students, Function<String, Group> function, ToIntFunction<Group> functionInt) {
        return allGroupsStream(students)
                .map(function)
                .sorted(Comparator.comparingInt(functionInt)
                        .reversed()
                        .thenComparing(Group::getName))
                .map(Group::getName)
                .findFirst()
                .orElse("");
    }



    public static void main(String[] args) {
        TreeSet<Integer> treeSet = new TreeSet<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });
        Scanner scanner = new Scanner(System.in);
        for (int i = 0; i < 10; i++) {
            treeSet.add(scanner.nextInt());
        }

        for (int i : treeSet
        ) {
            System.out.println(i);
        }
    }
}
