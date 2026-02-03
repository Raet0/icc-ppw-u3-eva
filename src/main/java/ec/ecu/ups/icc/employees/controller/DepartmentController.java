package ec.ecu.ups.icc.employees.controller;

import ec.ecu.ups.icc.employees.dto.DepartmentWithEmployeesDto;
import ec.ecu.ups.icc.employees.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    
    private final EmployeeService employeeService;
    
    public DepartmentController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    
    @GetMapping("/{id}/employees")
    public ResponseEntity<DepartmentWithEmployeesDto> getDepartmentWithEmployees(
            @PathVariable Long id,
            @RequestParam(defaultValue = "desc") String sort) {
        
        DepartmentWithEmployeesDto response = employeeService.getDepartmentWithEmployees(id, sort);
        return ResponseEntity.ok(response);
    }
}
