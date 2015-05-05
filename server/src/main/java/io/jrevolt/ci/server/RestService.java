package io.jrevolt.ci.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
@Path("main")
@Api("main")
//@Consumes("application/json")
//@Produces("application/json")
public class RestService {

    static public class User {

        @FormParam("name")
        @ApiParam
        String name;

        @FormParam("surname")
        @ApiParam
        String surname;

        @FormParam("address")
        @ApiParam
        Address address;

        public User() {
        }

        public User(String name, String surname) {
            this.name = name;
            this.surname = surname;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    static public class Address {

        @FormParam("street")
        String street;

        @FormParam("city")
        String city;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    @POST
    @Path("hello")
    @ApiOperation(value = "hello", response = User.class)
    public User hello(
            @BeanParam @ApiParam User msg
    ) {
//        return Response.ok("Hello!").build();
//        return Response.ok("{ \"key\": \"value\" }").build();
        return new User(msg.getName(), msg.getSurname());
    }


}
