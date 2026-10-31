package org.example.university.controller;

import java.util.List;
import org.example.university.model.Teacher;
import org.example.university.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/teachers")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherController {

    private final TeacherRepository teacherRepository;

    public TeacherController(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @GetMapping
    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    @GetMapping("/{id}")
    public Teacher findById(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Teacher create(@RequestBody Teacher teacher) {
        return teacherRepository.save(teacher);
    }

    @PutMapping("/{id}")
    public Teacher update(@PathVariable Long id, @RequestBody Teacher teacher) {
        if (!teacherRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        teacher.setId(id);
        return teacherRepository.save(teacher);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!teacherRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        teacherRepository.deleteById(id);
    }
}
