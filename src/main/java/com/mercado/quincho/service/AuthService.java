package com.mercado.quincho.service;

import com.mercado.quincho.entity.Role;
import com.mercado.quincho.entity.User;
import com.mercado.quincho.exception.MyException;
import com.mercado.quincho.repository.UserRepository;
import com.mercado.quincho.request.LoginRequest;
import com.mercado.quincho.request.RegisterRequest;
import com.mercado.quincho.response.AuthResponse;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación y registro de usuarios.
 * Esta clase contieme métodos para registrar nuevos usuarios en el sistema,
 * para loguearse y realizar la validadción de los datos de registro. También
 * de tokens de autenticación.
 * 
 * @author QuinSDev
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final JwtService jwtService;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;
    
    /**
     * Realiza la autentticación del usuario basado en las credenciales 
     * proporcionadas.
     * 
     * @param request: La solicitud de inicio de sesión que contiene el email
     * y la contraseña.
     * @return Una respuesta de autenticación que incluye un token si la 
     * autenticación es exitosa, o un mensaje de error en caso contrario.
     */
    @Transactional
    public AuthResponse loginUser(LoginRequest request) {
        // Autentica al usuario utilizando Spring Security
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword()));
        
        // Obtiene los detalle del usuario a partir del repositorio
        UserDetails user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        
        // Genera un token de autenticación y lo devuelve.
        String token = jwtService.getToken(user);
        return AuthResponse.builder()
                .token(token)
                .build();
    }
    
    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param request: La solicitud de registro que contiene los datos de un
     * nuevo usuario.
     * @return Una respuesta de autenticación que incluye un token si el 
     * registro es exitoso, o un mensaje de error en caso contrario.
     */
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {

        try {
            validUser(request);
            
            // Crea un objeto User con los datos proporcionados por el usuario.
            User user = User.builder()
                    .email(request.getEmail())
                    .name(request.getName())
                    .lastName(request.getLastName())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .address(request.getAddress())
                    .phoneNumber(request.getPhoneNumber())
                    .role(Role.CUSTOMER)
                    .build();

            // Guarda el usuario en la base de datos.
            userRepository.save(user);

            // Devuelve el token de auntenticación para el usuario registrado.
            return AuthResponse.builder()
                    .token(jwtService.getToken(user))
                    .build();

        } catch (MyException ex) {
            return new AuthResponse("Error en el registro: " + ex.getMessage());
        }

    }
    
    /**
     * Realiza la validación de los datos de registro.
     * 
     * @param request: La solicitud de registro que se va a validar.
     * @throws MyException: Si se encuentran problemas en los datos de registro,
     * se lanza una excepción con un mensaje descriptivo del problema.
     */
    @Transactional
    public void validUser(RegisterRequest request) throws MyException {

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new MyException("El email no puede estar vacío");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new MyException("La contraseña no puede estar vacía");
        }
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new MyException("El nombre no puede estar vacío");
        }
        if (request.getLastName() == null || request.getLastName().isEmpty()) {
            throw new MyException("El apellido no puede estar vacío");
        }

        if (request.getPassword().length() <= 5) {
            throw new MyException("La contraseña no debe tener menos de 5 caracteres");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new MyException("Las contraseñas no coinciden");
        }

        if (request.getAddress() == null || request.getAddress().isEmpty()) {
            throw new MyException("La dirección no puede estar vacía o ser nula");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
            throw new MyException("El número de teléfono no puede estar vacío"
                    + " o estar nulo");
        }

    }
}